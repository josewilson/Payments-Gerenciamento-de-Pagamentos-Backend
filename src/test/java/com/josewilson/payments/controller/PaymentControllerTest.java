package com.josewilson.payments.controller;


import tools.jackson.databind.ObjectMapper;
import com.josewilson.payments.domain.PaymentStatus;
import com.josewilson.payments.dto.PaymentRequestDTO;
import com.josewilson.payments.dto.PaymentResponseDTO;
import com.josewilson.payments.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Deve criar um pagamento com sucesso")
    void shouldCreatePaymentSuccessfully() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setCustomerName("Francisco Almeida");
        request.setCustomerEmail("francisco@email.com");
        request.setAmount(new BigDecimal("199.90"));
        request.setCurrency("BRL");
        request.setExternalReference("PEDIDO-123");

        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setId(1L);
        response.setCustomerName("Francisco Almeida");
        response.setCustomerEmail("francisco@email.com");
        response.setAmount(new BigDecimal("199.90"));
        response.setCurrency("BRL");
        response.setStatus(PaymentStatus.PENDING);
        response.setExternalReference("PEDIDO-123");
        response.setExchangeRateToBrl(BigDecimal.ONE);

        when(paymentService.createPayment(eq("key-1"), any(PaymentRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/payments")
                        .header("Idempotency-Key", "key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.customerName").value("Francisco Almeida"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.exchangeRateToBrl").value(1.0));
    }

    @Test
    @DisplayName("Deve rejeitar criação sem Idempotency-Key")
    void shouldRejectMissingIdempotencyKey() throws Exception {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setCustomerName("Francisco Almeida");
        request.setCustomerEmail("francisco@email.com");
        request.setAmount(new BigDecimal("10.00"));
        request.setCurrency("BRL");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
