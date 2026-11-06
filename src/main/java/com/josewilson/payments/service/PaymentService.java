package com.josewilson.payments.service;


import com.josewilson.payments.client.ExchangeRateClient;
import com.josewilson.payments.domain.OutboxEvent;
import com.josewilson.payments.domain.Payment;
import com.josewilson.payments.domain.PaymentAudit;
import com.josewilson.payments.domain.PaymentStatus;
import com.josewilson.payments.domain.PaymentStatusChangedEvent;
import com.josewilson.payments.dto.PaymentRequestDTO;
import com.josewilson.payments.dto.PaymentResponseDTO;
import com.josewilson.payments.dto.PaymentStatusUpdateDTO;
import com.josewilson.payments.exception.InvalidStatusTransitionException;
import com.josewilson.payments.exception.ResourceNotFoundException;
import com.josewilson.payments.mapper.PaymentMapper;
import com.josewilson.payments.repository.OutboxEventRepository;
import com.josewilson.payments.repository.PaymentAuditRepository;
import com.josewilson.payments.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAuditRepository paymentAuditRepository;
    private final PaymentMapper paymentMapper;
    private final ExchangeRateClient exchangeRateClient;
    private final MeterRegistry meterRegistry;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;
    private final TransactionTemplate transactionTemplate;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentAuditRepository paymentAuditRepository,
            PaymentMapper paymentMapper,
            ExchangeRateClient exchangeRateClient,
            MeterRegistry meterRegistry,
            OutboxEventRepository outboxEventRepository,
            JsonMapper jsonMapper,
            TransactionTemplate transactionTemplate
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAuditRepository = paymentAuditRepository;
        this.paymentMapper = paymentMapper;
        this.exchangeRateClient = exchangeRateClient;
        this.meterRegistry = meterRegistry;
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public PaymentResponseDTO createPayment(String idempotencyKey, PaymentRequestDTO request) {
        Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .externalReference(request.getExternalReference())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Payment savedPayment;
        try {
            savedPayment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException ex) {
            // corrida: outra requisicao com a mesma chave gravou primeiro (indice unico)
            return paymentRepository.findByIdempotencyKey(idempotencyKey)
                    .map(this::toResponse)
                    .orElseThrow(() -> ex);
        }

        saveAudit(savedPayment.getId(), "CREATED", "Pagamento criado com status PENDING.");
        meterRegistry.counter("payments.creations").increment();

        return toResponse(savedPayment);
    }

    // BRL não passa pelo cliente externo (cache/retry/circuit breaker): o circuito aberto não pode derrubar pagamentos em BRL.
    private BigDecimal rateFor(String currency) {
        return "BRL".equalsIgnoreCase(currency) ? BigDecimal.ONE : exchangeRateClient.getBrlExchangeRate(currency);
    }

    private PaymentResponseDTO toResponse(Payment payment) {
        return paymentMapper.toResponseDTO(payment, rateFor(payment.getCurrency()));
    }

    public Page<PaymentResponseDTO> findPayments(PaymentStatus status, Pageable pageable) {
        Page<Payment> paymentsPage;

        if (status != null) {
            paymentsPage = paymentRepository.findByStatus(status, pageable);
        } else {
            paymentsPage = paymentRepository.findAll(pageable);
        }

        return paymentsPage.map(payment -> {
            BigDecimal exchangeRate = rateFor(payment.getCurrency());
            return paymentMapper.toResponseDTO(payment, exchangeRate);
        });
    }

    public PaymentResponseDTO findPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com o id: " + id));

        BigDecimal exchangeRate = rateFor(payment.getCurrency());

        return paymentMapper.toResponseDTO(payment, exchangeRate);
    }

    public PaymentResponseDTO updatePaymentStatus(Long id, PaymentStatusUpdateDTO request) {
        // Transação curta só com o banco (sem a chamada HTTP de câmbio): status e evento do outbox são atômicos.
        Payment updatedPayment = transactionTemplate.execute(tx -> {
            Payment payment = paymentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com o id: " + id));

            PaymentStatus previousStatus = payment.getStatus();
            if (!previousStatus.canTransitionTo(request.getStatus())) {
                throw new InvalidStatusTransitionException(previousStatus, request.getStatus());
            }

            payment.setStatus(request.getStatus());
            payment.setUpdatedAt(LocalDateTime.now());

            Payment saved = paymentRepository.save(payment);
            outboxEventRepository.save(statusChangedEvent(saved, previousStatus));
            return saved;
        });

        saveAudit(
                updatedPayment.getId(),
                "STATUS_UPDATED",
                "Status alterado para " + updatedPayment.getStatus()
        );

        meterRegistry.counter("payments.status.changed", "status", updatedPayment.getStatus().name()).increment();

        BigDecimal exchangeRate = rateFor(updatedPayment.getCurrency());

        return paymentMapper.toResponseDTO(updatedPayment, exchangeRate);
    }

    private OutboxEvent statusChangedEvent(Payment payment, PaymentStatus previousStatus) {
        var event = new PaymentStatusChangedEvent(UUID.randomUUID(), payment.getId(), previousStatus,
                payment.getStatus(), payment.getAmount(), payment.getCurrency(), Instant.now());

        return OutboxEvent.builder()
                .id(event.eventId())
                .aggregateId(payment.getId())
                .type(PaymentStatusChangedEvent.TYPE)
                .payload(jsonMapper.writeValueAsString(event))
                .createdAt(LocalDateTime.now())
                .build();
    }

    public List<PaymentAudit> findAuditByPaymentId(Long paymentId) {
        return paymentAuditRepository.findByPaymentId(paymentId);
    }

    private void saveAudit(Long paymentId, String action, String details) {
        PaymentAudit audit = PaymentAudit.builder()
                .paymentId(paymentId)
                .action(action)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

        paymentAuditRepository.save(audit);
    }
}