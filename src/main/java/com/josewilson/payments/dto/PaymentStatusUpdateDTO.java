package com.josewilson.payments.dto;


import com.josewilson.payments.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public class PaymentStatusUpdateDTO {

    @NotNull(message = "O novo status é obrigatório.")
    private PaymentStatus status;

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}
