package com.josewilson.payments.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PaymentIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");
    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @RegisterExtension
    static WireMockExtension fx = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.mongodb.uri", () -> mongo.getReplicaSetUrl("payments_audit_test"));
        r.add("exchange-rate.base-url", fx::baseUrl);
        r.add("resilience4j.retry.instances.exchangeRate.wait-duration", () -> "10ms");
        r.add("resilience4j.circuitbreaker.instances.exchangeRate.minimum-number-of-calls", () -> "100");
    }

    @Autowired
    MockMvc mvc;

    private static String body(String currency) {
        return """
                {"customerName":"Ana","customerEmail":"ana@email.com","amount":150.50,
                 "currency":"%s","externalReference":"PED-1"}""".formatted(currency);
    }

    private MvcResult create(String key, String currency) throws Exception {
        return mvc.perform(post("/api/payments").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body(currency))).andReturn();
    }

    @Test
    void createIsIdempotentAndCachesRate() throws Exception {
        fx.stubFor(WireMock.get("/latest/USD").willReturn(okJson("{\"rates\":{\"BRL\":5.25}}")));

        MvcResult first = create("it-idem-1", "usd");
        MvcResult second = create("it-idem-1", "USD");

        assertEquals(201, first.getResponse().getStatus());
        assertEquals(201, second.getResponse().getStatus());
        Integer id = JsonPath.read(first.getResponse().getContentAsString(), "$.id");
        assertEquals(id, JsonPath.read(second.getResponse().getContentAsString(), "$.id"));
        assertEquals(5.25, ((Number) JsonPath.read(first.getResponse().getContentAsString(), "$.exchangeRateToBrl")).doubleValue());
        fx.verify(1, getRequestedFor(urlEqualTo("/latest/USD")));
    }

    @Test
    void statusFlowWithAuditAndConflict() throws Exception {
        Integer id = JsonPath.read(create("it-flow-1", "BRL").getResponse().getContentAsString(), "$.id");

        mvc.perform(patch("/api/payments/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(patch("/api/payments/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/payments/{id}/audit", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[1].action").value("STATUS_UPDATED"));
    }

    @Test
    void missingIdempotencyKeyIs400AndUnknownPaymentIs404() throws Exception {
        mvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(body("BRL")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/payments/{id}", 999999)).andExpect(status().isNotFound());
    }

    @Test
    void exchangeOutageIs503ThenRetryWithSameKeyReturnsSamePayment() throws Exception {
        fx.stubFor(WireMock.get("/latest/GBP").willReturn(serverError()));
        MvcResult down = create("it-outage-1", "GBP");
        assertEquals(503, down.getResponse().getStatus());

        fx.stubFor(WireMock.get("/latest/GBP").willReturn(okJson("{\"rates\":{\"BRL\":6.9}}")));
        MvcResult up = create("it-outage-1", "GBP");
        assertEquals(201, up.getResponse().getStatus());

        // só um pagamento foi criado para a chave
        mvc.perform(get("/api/payments?size=100")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.currency=='GBP')]", hasSize(1)));
    }
}
