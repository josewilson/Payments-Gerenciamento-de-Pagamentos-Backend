package com.josewilson.payments.client;

import com.josewilson.payments.exception.CotacaoIndisponivelException;
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
    public BigDecimal getBrlExchangeRate(String currency) {
        if ("BRL".equalsIgnoreCase(currency)) {
            return BigDecimal.ONE;
        }

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
}
