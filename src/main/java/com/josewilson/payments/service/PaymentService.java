package com.josewilson.payments.service;


import com.josewilson.payments.client.ExchangeRateClient;
import com.josewilson.payments.domain.Payment;
import com.josewilson.payments.domain.PaymentAudit;
import com.josewilson.payments.domain.PaymentStatus;
import com.josewilson.payments.dto.PaymentRequestDTO;
import com.josewilson.payments.dto.PaymentResponseDTO;
import com.josewilson.payments.dto.PaymentStatusUpdateDTO;
import com.josewilson.payments.exception.ResourceNotFoundException;
import com.josewilson.payments.mapper.PaymentMapper;
import com.josewilson.payments.repository.PaymentAuditRepository;
import com.josewilson.payments.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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

    public PaymentResponseDTO createPayment(PaymentRequestDTO request) {
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .amount(request.getAmount())
                .currency(request.getCurrency().toUpperCase())
                .status(PaymentStatus.PENDING)
                .externalReference(request.getExternalReference())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        saveAudit(savedPayment.getId(), "CREATED", "Pagamento criado com status PENDING.");

        Double exchangeRate = exchangeRateClient.getBrlExchangeRate(savedPayment.getCurrency());

        return paymentMapper.toResponseDTO(savedPayment, exchangeRate);
    }

    public Page<PaymentResponseDTO> findPayments(PaymentStatus status, Pageable pageable) {
        Page<Payment> paymentsPage;

        if (status != null) {
            paymentsPage = paymentRepository.findByStatus(status, pageable);
        } else {
            paymentsPage = paymentRepository.findAll(pageable);
        }

        return paymentsPage.map(payment -> {
            Double exchangeRate = exchangeRateClient.getBrlExchangeRate(payment.getCurrency());
            return paymentMapper.toResponseDTO(payment, exchangeRate);
        });
    }

    public PaymentResponseDTO findPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com o id: " + id));

        Double exchangeRate = exchangeRateClient.getBrlExchangeRate(payment.getCurrency());

        return paymentMapper.toResponseDTO(payment, exchangeRate);
    }

    public PaymentResponseDTO updatePaymentStatus(Long id, PaymentStatusUpdateDTO request) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado com o id: " + id));

        payment.setStatus(request.getStatus());
        payment.setUpdatedAt(LocalDateTime.now());

        Payment updatedPayment = paymentRepository.save(payment);

        saveAudit(
                updatedPayment.getId(),
                "STATUS_UPDATED",
                "Status alterado para " + updatedPayment.getStatus()
        );

        Double exchangeRate = exchangeRateClient.getBrlExchangeRate(updatedPayment.getCurrency());

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