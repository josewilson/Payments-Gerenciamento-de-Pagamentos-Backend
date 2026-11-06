package com.josewilson.payments.client;

import com.josewilson.payments.exception.CotacaoIndisponivelException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Component
public class ExchangeRateClient {

    private final RestClient restClient;

    public ExchangeRateClient(RestClient exchangeRateRestClient) {
        this.restClient = exchangeRateRestClient;
    }

    @Cacheable("cotacoes")
    @Retry(name = "exchangeRate")
    @CircuitBreaker(name = "exchangeRate", fallbackMethod = "circuitOpen")
    public BigDecimal getBrlExchangeRate(String currency) {
        ExchangeRateResponse response;
        try {
            response = restClient.get()
                    .uri("/latest/{base}", currency.toUpperCase())
                    .retrieve()
                    .body(ExchangeRateResponse.class);
        } catch (RestClientException ex) {
            throw new CotacaoIndisponivelException("Falha ao consultar cotação de " + currency, ex);
        }

        BigDecimal rate = response == null || response.rates() == null ? null : response.rates().get("BRL");
        if (rate == null) {
            throw new CotacaoIndisponivelException("Cotação BRL ausente na resposta para " + currency);
        }
        return rate;
    }

    // Circuito aberto: falha rápido com a mesma exceção de domínio (-> 503).
    @SuppressWarnings("unused")
    private BigDecimal circuitOpen(String currency, CallNotPermittedException ex) {
        throw new CotacaoIndisponivelException("Circuito de cotação aberto para " + currency, ex);
    }
}
