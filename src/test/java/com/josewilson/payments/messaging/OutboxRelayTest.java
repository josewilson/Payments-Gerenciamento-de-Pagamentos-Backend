package com.josewilson.payments.messaging;

import com.josewilson.payments.domain.OutboxEvent;
import com.josewilson.payments.repository.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    OutboxEventRepository repository;
    @Mock
    RabbitTemplate rabbitTemplate;

    SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private OutboxEvent event() {
        return OutboxEvent.builder().id(UUID.randomUUID()).aggregateId(1L).type("PagamentoStatusAlterado")
                .payload("{}").createdAt(LocalDateTime.now()).build();
    }

    @Test
    void marksPublishedOnSuccess() {
        OutboxEvent e1 = event();
        when(repository.lockPending(50)).thenReturn(List.of(e1));
        when(rabbitTemplate.invoke(any())).thenReturn(true);

        new OutboxRelay(repository, rabbitTemplate, registry).publishPending();

        assertNotNull(e1.getPublishedAt());
        assertEquals(1.0, registry.counter("payments.events.published").count());
    }

    @Test
    void brokerFailureKeepsEventPendingAndStopsBatch() {
        OutboxEvent e1 = event();
        OutboxEvent e2 = event();
        OutboxEvent e3 = event();
        when(repository.lockPending(50)).thenReturn(List.of(e1, e2, e3));
        when(rabbitTemplate.invoke(any())).thenReturn(true).thenThrow(new AmqpException("broker fora do ar"));

        new OutboxRelay(repository, rabbitTemplate, registry).publishPending();

        assertNotNull(e1.getPublishedAt());
        assertNull(e2.getPublishedAt());
        assertNull(e3.getPublishedAt());
        assertEquals(1.0, registry.counter("payments.events.publish.failed").count());
    }
}
