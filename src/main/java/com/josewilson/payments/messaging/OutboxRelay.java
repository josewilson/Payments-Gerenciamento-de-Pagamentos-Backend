package com.josewilson.payments.messaging;

import com.josewilson.payments.domain.OutboxEvent;
import com.josewilson.payments.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Publica os eventos pendentes do outbox no RabbitMQ (entrega at-least-once, com confirmação do broker).
 * Se o broker falhar, o lote para e o evento fica pendente para a próxima rodada.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH_SIZE = 50;
    private static final long CONFIRM_TIMEOUT_MS = 5_000;

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    public OutboxRelay(OutboxEventRepository repository, RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
    }

    // ponytail: eventos publicados nunca são apagados; adicionar job de limpeza quando a tabela crescer.
    @Scheduled(fixedDelayString = "${outbox.poll-interval:1s}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : repository.lockPending(BATCH_SIZE)) {
            try {
                publish(event);
            } catch (Exception ex) {
                meterRegistry.counter("payments.events.publish.failed").increment();
                log.warn("Falha ao publicar evento {}; será tentado de novo: {}", event.getId(), ex.getMessage());
                return;
            }
            event.setPublishedAt(LocalDateTime.now());
            meterRegistry.counter("payments.events.published").increment();
        }
    }

    private void publish(OutboxEvent event) {
        rabbitTemplate.invoke(ops -> {
            ops.convertAndSend(MessagingConfig.EXCHANGE, MessagingConfig.ROUTING_KEY, event.getPayload(), message -> {
                message.getMessageProperties().setContentType("application/json");
                message.getMessageProperties().setMessageId(event.getId().toString());
                message.getMessageProperties().setType(event.getType());
                return message;
            });
            ops.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
            return true;
        });
    }
}
