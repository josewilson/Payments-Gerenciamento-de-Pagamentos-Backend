package com.josewilson.payments.config;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.josewilson.payments.client.ExchangeRateClient;
import com.josewilson.payments.exception.CotacaoIndisponivelException;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = CotacaoResilienceTest.Config.class, properties = {
        "resilience4j.retry.instances.exchangeRate.max-attempts=3",
        "resilience4j.retry.instances.exchangeRate.wait-duration=10ms",
        "resilience4j.retry.instances.exchangeRate.retry-exceptions=com.josewilson.payments.exception.CotacaoIndisponivelException",
        "resilience4j.circuitbreaker.instances.exchangeRate.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.exchangeRate.minimum-number-of-calls=4",
        "resilience4j.circuitbreaker.instances.exchangeRate.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.exchangeRate.wait-duration-in-open-state=1h",
        "resilience4j.circuitbreaker.instances.exchangeRate.record-exceptions=com.josewilson.payments.exception.CotacaoIndisponivelException"
})
class CotacaoResilienceTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @Configuration
    @EnableAspectJAutoProxy
    @ImportAutoConfiguration({CircuitBreakerAutoConfiguration.class, RetryAutoConfiguration.class})
    static class Config {
        @Bean
        ExchangeRateClient exchangeRateClient() {
            return new ExchangeRateClient(RestClient.builder().baseUrl(wm.baseUrl()).build());
        }
    }

    @Autowired
    ExchangeRateClient client;

    @Test
    void retriesTransientFailureThenOpensCircuit() {
        // 2 falhas e depois sucesso: o retry absorve
        wm.stubFor(get("/latest/EUR").inScenario("s").whenScenarioStateIs("Started")
                .willReturn(serverError()).willSetStateTo("f2"));
        wm.stubFor(get("/latest/EUR").inScenario("s").whenScenarioStateIs("f2")
                .willReturn(serverError()).willSetStateTo("ok"));
        wm.stubFor(get("/latest/EUR").inScenario("s").whenScenarioStateIs("ok")
                .willReturn(okJson("{\"rates\":{\"BRL\":6}}")));

        assertEquals(new BigDecimal("6"), client.getBrlExchangeRate("EUR"));
        wm.verify(3, getRequestedFor(urlEqualTo("/latest/EUR")));

        // janela do circuito agora: F,F,S. Falha constante: a 4a chamada abre o circuito
        wm.stubFor(get("/latest/USD").willReturn(serverError()));
        assertThrows(CotacaoIndisponivelException.class, () -> client.getBrlExchangeRate("USD"));
        int callsUntilOpen = wm.countRequestsMatching(getRequestedFor(urlEqualTo("/latest/USD")).build()).getCount();

        // circuito aberto: falha rápido, sem chamar a API
        assertThrows(CotacaoIndisponivelException.class, () -> client.getBrlExchangeRate("USD"));
        wm.verify(callsUntilOpen, getRequestedFor(urlEqualTo("/latest/USD")));
        assertEquals(1, callsUntilOpen);
    }
}
