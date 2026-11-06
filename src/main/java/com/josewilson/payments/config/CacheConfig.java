package com.josewilson.payments.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

// Cache é o aspecto mais externo: hits não passam por retry/circuit breaker
// e não diluem a taxa de falha do circuito.
@Configuration
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfig {
}
