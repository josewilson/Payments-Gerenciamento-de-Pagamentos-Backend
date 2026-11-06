package com.josewilson.payments.service;

import com.josewilson.payments.client.ExchangeRateClient;
import com.josewilson.payments.domain.Payment;
import com.josewilson.payments.domain.PaymentAudit;
import com.josewilson.payments.domain.PaymentStatus;
import com.josewilson.payments.dto.PaymentStatusUpdateDTO;
import com.josewilson.payments.exception.InvalidStatusTransitionException;
import com.josewilson.payments.exception.ResourceNotFoundException;
import com.josewilson.payments.mapper.PaymentMapper;
import com.josewilson.payments.domain.OutboxEvent;
import com.josewilson.payments.repository.OutboxEventRepository;
import com.josewilson.payments.repository.PaymentAuditRepository;
import com.josewilson.payments.repository.PaymentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentStatusFlowTest {

    @Mock
    PaymentRepository paymentRepository;
    @Mock
    PaymentAuditRepository paymentAuditRepository;
    @Mock
    ExchangeRateClient exchangeRateClient;
    @Mock
    OutboxEventRepository outboxEventRepository;

    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, paymentAuditRepository, new PaymentMapper(), exchangeRateClient,
                registry, outboxEventRepository, JsonMapper.builder().build(),
                new TransactionTemplate(mock(PlatformTransactionManager.class)));
    }

    private Payment payment(PaymentStatus status) {
        return Payment.builder().id(1L).customerName("A").customerEmail("a@a.com")
                .amount(new BigDecimal("10.00")).currency("BRL").status(status)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    private PaymentStatusUpdateDTO update(PaymentStatus status) {
        PaymentStatusUpdateDTO dto = new PaymentStatusUpdateDTO();
        dto.setStatus(status);
        return dto;
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
    void pendingTransitionsAndWritesAudit(PaymentStatus target) {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(PaymentStatus.PENDING)));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        var result = service.updatePaymentStatus(1L, update(target));

        assertEquals(target, result.getStatus());
        ArgumentCaptor<PaymentAudit> audit = ArgumentCaptor.forClass(PaymentAudit.class);
        verify(paymentAuditRepository).save(audit.capture());
        assertEquals(1L, audit.getValue().getPaymentId());
        assertEquals("STATUS_UPDATED", audit.getValue().getAction());
        assertTrue(audit.getValue().getDetails().contains(target.name()));
        assertEquals(1.0, registry.counter("payments.status.changed", "status", target.name()).count());

        ArgumentCaptor<OutboxEvent> outbox = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outbox.capture());
        assertEquals("PagamentoStatusAlterado", outbox.getValue().getType());
        assertEquals(1L, outbox.getValue().getAggregateId());
        assertTrue(outbox.getValue().getPayload().contains("\"previousStatus\":\"PENDING\""));
        assertTrue(outbox.getValue().getPayload().contains("\"newStatus\":\"" + target.name() + "\""));
        assertNull(outbox.getValue().getPublishedAt());
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
    void terminalStatusCannotChangeAndWritesNothing(PaymentStatus from) {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment(from)));

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.updatePaymentStatus(1L, update(PaymentStatus.CANCELLED)));

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(paymentAuditRepository, outboxEventRepository);
    }

    @Test
    void unknownPaymentThrowsNotFound() {
        when(paymentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updatePaymentStatus(9L, update(PaymentStatus.APPROVED)));
    }
}
