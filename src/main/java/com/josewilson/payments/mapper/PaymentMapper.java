package com.josewilson.payments.mapper;


import com.josewilson.payments.domain.Payment;
import com.josewilson.payments.dto.PaymentResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponseDTO toResponseDTO(Payment payment) {
        return toResponseDTO(payment, null);
    }

    public PaymentResponseDTO toResponseDTO(Payment payment, Double exchangeRateToBrl) {
        PaymentResponseDTO response = new PaymentResponseDTO();
        response.setId(payment.getId());
        response.setCustomerName(payment.getCustomerName());
        response.setCustomerEmail(payment.getCustomerEmail());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setExternalReference(payment.getExternalReference());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        response.setExchangeRateToBrl(exchangeRateToBrl);

        return response;
    }
}