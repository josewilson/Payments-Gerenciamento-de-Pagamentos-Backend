package com.josewilson.payments.service;


import com.josewilson.payments.client.ExchangeRateClient;
import com.josewilson.payments.domain.Payment;
import com.josewilson.payments.domain.PaymentStatus;
import com.josewilson.payments.dto.PaymentRequestDTO;
import com.josewilson.payments.dto.PaymentResponseDTO;
import com.josewilson.payments.mapper.PaymentMapper;
import com.josewilson.payments.repository.PaymentAuditRepository;
import com.josewilson.payments.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentAuditRepository paymentAuditRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @InjectMocks
    private PaymentService paymentService;

    private PaymentRequestDTO paymentRequestDTO;
    private Payment payment;
    private PaymentResponseDTO paymentResponseDTO;

    @BeforeEach
    void setUp() {
        paymentRequestDTO = new PaymentRequestDTO();
        paymentRequestDTO.setCustomerName("Francisco Almeida");
        paymentRequestDTO.setCustomerEmail("francisco@email.com");
        paymentRequestDTO.setAmount(new BigDecimal("199.90"));
        paymentRequestDTO.setCurrency("brl");
        paymentRequestDTO.setExternalReference("PEDIDO-123");

        payment = Payment.builder()
                .id(1L)
                .customerName("Francisco Almeida")
                .customerEmail("francisco@email.com")
                .amount(new BigDecimal("199.90"))
                .currency("BRL")
                .status(PaymentStatus.PENDING)
                .externalReference("PEDIDO-123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentResponseDTO = new PaymentResponseDTO();
        paymentResponseDTO.setId(1L);
        paymentResponseDTO.setCustomerName("Francisco Almeida");
        paymentResponseDTO.setCustomerEmail("francisco@email.com");
        paymentResponseDTO.setAmount(new BigDecimal("199.90"));
        paymentResponseDTO.setCurrency("BRL");
        paymentResponseDTO.setStatus(PaymentStatus.PENDING);
        paymentResponseDTO.setExternalReference("PEDIDO-123");
        paymentResponseDTO.setExchangeRateToBrl(1.0);
    }

    @Test
    void shouldCreatePaymentSuccessfully() {
        when(paymentRepository.save(ArgumentMatchers.any(Payment.class))).thenReturn(payment);
        when(exchangeRateClient.getBrlExchangeRate("BRL")).thenReturn(1.0);
        when(paymentMapper.toResponseDTO(payment, 1.0)).thenReturn(paymentResponseDTO);

        PaymentResponseDTO result = paymentService.createPayment(paymentRequestDTO);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Francisco Almeida", result.getCustomerName());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(1.0, result.getExchangeRateToBrl());

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(paymentAuditRepository, times(1)).save(any());
        verify(exchangeRateClient, times(1)).getBrlExchangeRate("BRL");
        verify(paymentMapper, times(1)).toResponseDTO(payment, 1.0);
    }
}
