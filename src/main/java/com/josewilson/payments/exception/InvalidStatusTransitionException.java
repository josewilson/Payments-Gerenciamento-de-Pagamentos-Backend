package com.josewilson.payments.exception;

import com.josewilson.payments.domain.PaymentStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(PaymentStatus from, PaymentStatus to) {
        super("Transição de status inválida: " + from + " -> " + to);
    }
}
