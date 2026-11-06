package com.josewilson.payments.service;


import com.josewilson.payments.client.ExchangeRateClient;
import com.josewilson.payments.domain.Payment;
import com.josewilson.payments.domain.PaymentAudit;
import com.josewilson.payments.domain.PaymentStatus;
import com.josewilson.payments.dto.PaymentRequestDTO;
import com.josewilson.payments.dto.PaymentResponseDTO;
import com.josewilson.payments.dto.PaymentStatusUpdateDTO;
import com.josewilson.payments.exception.InvalidStatusTransitionException;
import com.josewilson.payments.exception.ResourceNotFoundException;
import com.josewilson.payments.mapper.PaymentMapper;
import com.josewilson.payments.repository.PaymentAuditRepository;
import com.josewilson.payments.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAuditRepository paymentAuditRepository;
    private final PaymentMapper paymentMapper;
    private final ExchangeRateClient exchangeRateClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentAuditRepository paymentAuditRepository,
            PaymentMapper paymentMapper,
            ExchangeRateClient exchangeRateClient
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentAuditRepository = paymentAuditRepository;
        this.paymentMapper = paymentMapper;
        this.exchangeRateClient = exchangeRateClient;
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

        return toResponse(savedPayment);
    }

    private PaymentResponseDTO toResponse(Payment payment) {
        return paymentMapper.toResponseDTO(payment, exchangeRateClient.getBrlExchangeRate(payment.getCurrency()));
    }

    public Page<PaymentResponseDTO> findPayments(PaymentStatus status, Pageable pageable) {
        Page<Payment> paymentsPage;

        if (status != null) {
            paymentsPage = paymentRepository.findByStatus(status, pageable);
        } else {
            paymentsPage = paymentRepository.findAll(pageable);
        }

        return paymentsPage.map(payment -> {
            BigDecimal exchangeRate = exchangeRateClient.getBrlExchangeRate(payment.getCurrency());
            return paymentMapper.toResponseDTO(payment, exchangeRate);
        });
    }

    public PaymentResponseDTO findPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com o id: " + id));

        BigDecimal exchangeRate = exchangeRateClient.getBrlExchangeRate(payment.getCurrency());

        return paymentMapper.toResponseDTO(payment, exchangeRate);
    }

    public PaymentResponseDTO updatePaymentStatus(Long id, PaymentStatusUpdateDTO request) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com o id: " + id));

        if (!payment.getStatus().canTransitionTo(request.getStatus())) {
            throw new InvalidStatusTransitionException(payment.getStatus(), request.getStatus());
        }

        payment.setStatus(request.getStatus());
        payment.setUpdatedAt(LocalDateTime.now());

        Payment updatedPayment = paymentRepository.save(payment);

        saveAudit(
                updatedPayment.getId(),
                "STATUS_UPDATED",
                "Status alterado para " + updatedPayment.getStatus()
        );

        BigDecimal exchangeRate = exchangeRateClient.getBrlExchangeRate(updatedPayment.getCurrency());

        return paymentMapper.toResponseDTO(updatedPayment, exchangeRate);
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