package com.josewilson.payments.controller;


import com.josewilson.payments.domain.PaymentAudit;
import com.josewilson.payments.domain.PaymentStatus;
import com.josewilson.payments.dto.PaymentRequestDTO;
import com.josewilson.payments.dto.PaymentResponseDTO;
import com.josewilson.payments.dto.PaymentStatusUpdateDTO;
import com.josewilson.payments.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public PaymentResponseDTO createPayment(@Valid @RequestBody PaymentRequestDTO request) {
        return paymentService.createPayment(request);
    }

    @GetMapping
    public Page<PaymentResponseDTO> findPayments(
            @RequestParam(required = false) PaymentStatus status,
            Pageable pageable
    ) {
        return paymentService.findPayments(status, pageable);
    }

    @GetMapping("/{id}")
    public PaymentResponseDTO findPaymentById(@PathVariable Long id) {
        return paymentService.findPaymentById(id);
    }

    @PatchMapping("/{id}/status")
    public PaymentResponseDTO updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody PaymentStatusUpdateDTO request
    ) {
        return paymentService.updatePaymentStatus(id, request);
    }

    @GetMapping("/{id}/audit")
    public List<PaymentAudit> findAuditByPaymentId(@PathVariable Long id) {
        return paymentService.findAuditByPaymentId(id);
    }
}