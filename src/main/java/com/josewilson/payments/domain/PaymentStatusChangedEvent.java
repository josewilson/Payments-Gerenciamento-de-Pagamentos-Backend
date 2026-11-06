package com.josewilson.payments.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Evento "PagamentoStatusAlterado". O eventId permite ao consumidor descartar duplicatas (entrega at-least-once). */
public record PaymentStatusChangedEvent(
        UUID eventId,
        Long paymentId,
        PaymentStatus previousStatus,
        PaymentStatus newStatus,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
    public static final String TYPE = "PagamentoStatusAlterado";
}
