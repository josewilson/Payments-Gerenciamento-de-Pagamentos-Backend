package com.josewilson.payments.config;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.josewilson.payments.client.ExchangeRateClient;
import com.josewilson.payments.exception.CotacaoIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(CotacaoCacheTest.Config.class)
class CotacaoCacheTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @Configuration
    @EnableCaching
    static class Config {
        @Bean
        CacheManager cacheManager() {
            return new CaffeineCacheManager("cotacoes");
        }

        @Bean
        ExchangeRateClient exchangeRateClient() {
            return new ExchangeRateClient(RestClient.builder().baseUrl(wm.baseUrl()).build());
        }
    }

    @Autowired
    ExchangeRateClient client;
    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void reset() {
        cacheManager.getCache("cotacoes").clear();
        wm.resetAll();
    }

    @Test
    void secondCallHitsCache() {
        wm.stubFor(get("/latest/USD").willReturn(okJson("{\"rates\":{\"BRL\":5.5}}")));

        assertEquals(new BigDecimal("5.5"), client.getBrlExchangeRate("USD"));
        assertEquals(new BigDecimal("5.5"), client.getBrlExchangeRate("USD"));

        wm.verify(1, getRequestedFor(urlEqualTo("/latest/USD")));
    }

    @Test
    void failuresAreNotCached() {
        wm.stubFor(get("/latest/USD").willReturn(serverError()));
        assertThrows(CotacaoIndisponivelException.class, () -> client.getBrlExchangeRate("USD"));

        wm.stubFor(get("/latest/USD").willReturn(okJson("{\"rates\":{\"BRL\":5.5}}")));
        assertEquals(new BigDecimal("5.5"), client.getBrlExchangeRate("USD"));
    }
}
