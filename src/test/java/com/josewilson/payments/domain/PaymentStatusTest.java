package com.josewilson.payments.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentStatusTest {

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
    void pendingCanMoveToAnyFinalStatus(PaymentStatus target) {
        assertTrue(PaymentStatus.PENDING.canTransitionTo(target));
    }

    @Test
    void pendingCannotStayPending() {
        assertFalse(PaymentStatus.PENDING.canTransitionTo(PaymentStatus.PENDING));
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
    void finalStatusesAreTerminal(PaymentStatus from) {
        for (PaymentStatus target : PaymentStatus.values()) {
            assertFalse(from.canTransitionTo(target));
        }
    }
}
