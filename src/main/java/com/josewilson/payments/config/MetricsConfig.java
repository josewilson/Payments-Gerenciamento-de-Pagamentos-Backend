package com.josewilson.payments.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// A auto-configuração de métricas do resilience4j-spring-boot3 não é registrada no Boot 4.
@Configuration
public class MetricsConfig {

    @Bean
    public MeterBinder circuitBreakerMetrics(CircuitBreakerRegistry registry) {
        return TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry);
    }

    @Bean
    public MeterBinder retryMetrics(RetryRegistry registry) {
        return TaggedRetryMetrics.ofRetryRegistry(registry);
    }
}
