package com.josewilson.payments.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.josewilson.payments.exception.CotacaoIndisponivelException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExchangeRateClientTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    private ExchangeRateClient clientWith(Duration readTimeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(readTimeout);
        return new ExchangeRateClient(RestClient.builder()
                .baseUrl(wm.baseUrl()).requestFactory(factory).build());
    }

    @Test
    void returnsBrlRateAsBigDecimal() {
        wm.stubFor(get("/latest/USD").willReturn(okJson("{\"result\":\"success\",\"rates\":{\"BRL\":5.4321,\"EUR\":0.9}}")));

        assertEquals(new BigDecimal("5.4321"), clientWith(Duration.ofSeconds(10)).getBrlExchangeRate("usd"));
    }

    @Test
    void serverErrorBecomesCotacaoIndisponivel() {
        wm.stubFor(get("/latest/USD").willReturn(serverError()));

        assertThrows(CotacaoIndisponivelException.class,
                () -> clientWith(Duration.ofSeconds(10)).getBrlExchangeRate("USD"));
    }

    @Test
    void missingBrlBecomesCotacaoIndisponivel() {
        wm.stubFor(get("/latest/USD").willReturn(okJson("{\"rates\":{\"EUR\":0.9}}")));

        assertThrows(CotacaoIndisponivelException.class,
                () -> clientWith(Duration.ofSeconds(10)).getBrlExchangeRate("USD"));
    }

    @Test
    void readTimeoutBecomesCotacaoIndisponivel() {
        // aquece o WireMock para o timeout curto medir só o atraso do stub
        wm.stubFor(get("/latest/EUR").willReturn(okJson("{\"rates\":{\"BRL\":6}}")));
        clientWith(Duration.ofSeconds(10)).getBrlExchangeRate("EUR");

        wm.stubFor(get("/latest/USD").willReturn(okJson("{\"rates\":{\"BRL\":5}}").withFixedDelay(3000)));

        assertThrows(CotacaoIndisponivelException.class,
                () -> clientWith(Duration.ofSeconds(1)).getBrlExchangeRate("USD"));
    }
}
