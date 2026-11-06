package com.josewilson.payments.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class MessagingConfig {

    public static final String EXCHANGE = "payments.events";
    public static final String ROUTING_KEY = "payment.status.changed";
    public static final String QUEUE = "payments.status-changed";

    @Bean
    TopicExchange paymentsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    // Fila durável: eventos não se perdem enquanto não houver consumidor.
    @Bean
    Queue statusChangedQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    Binding statusChangedBinding(Queue statusChangedQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(statusChangedQueue).to(paymentsExchange).with(ROUTING_KEY);
    }
}
