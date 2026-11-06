package com.josewilson.payments.domain;

public enum PaymentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED;

    /** Somente PENDING pode mudar; os demais estados são terminais. */
    public boolean canTransitionTo(PaymentStatus target) {
        return this == PENDING && target != PENDING;
    }
}
