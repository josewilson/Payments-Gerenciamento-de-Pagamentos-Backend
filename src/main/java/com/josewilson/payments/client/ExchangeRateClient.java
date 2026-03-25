package com.josewilson.payments.client;


import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class ExchangeRateClient {

    private final RestTemplate restTemplate;

    public ExchangeRateClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Double getBrlExchangeRate(String currency) {
        try {
            if ("BRL".equalsIgnoreCase(currency)) {
                return 1.0;
            }

            String url = "https://api.exchangerate.host/latest?base=" + currency + "&symbols=BRL";

            Map response = restTemplate.getForObject(url, Map.class);

            if (response == null || response.get("rates") == null) {
                return null;
            }

            Map rates = (Map) response.get("rates");
            Object brlValue = rates.get("BRL");

            if (brlValue instanceof Number number) {
                return number.doubleValue();
            }

            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}