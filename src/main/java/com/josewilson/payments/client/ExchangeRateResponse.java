package com.josewilson.payments.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeRateResponse(Map<String, BigDecimal> rates) {
}
