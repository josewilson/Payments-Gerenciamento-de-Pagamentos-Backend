package com.josewilson.payments.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.josewilson.payments.messaging.MessagingConfig;
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

    static final String SECRET = "integration-test-secret-with-at-least-32-bytes";

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");
    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");
    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management");

    @RegisterExtension
    static WireMockExtension fx = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("security.jwt.secret", () -> SECRET);
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.mongodb.uri", () -> mongo.getReplicaSetUrl("payments_audit_test"));
        r.add("exchange-rate.base-url", fx::baseUrl);
        r.add("spring.rabbitmq.host", rabbit::getHost);
        r.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        r.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        r.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        r.add("outbox.poll-interval", () -> "200ms");
        r.add("resilience4j.retry.instances.exchangeRate.wait-duration", () -> "10ms");
        r.add("resilience4j.circuitbreaker.instances.exchangeRate.minimum-number-of-calls", () -> "100");
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    RabbitTemplate rabbitTemplate;

    private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token(SECRET)));
    }

    private static String token(String secret) throws Exception {
        var claims = new JWTClaimsSet.Builder().subject("it")
                .expirationTime(new Date(System.currentTimeMillis() + 3_600_000)).build();
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    private static String body(String currency) {
        return """
                {"customerName":"Ana","customerEmail":"ana@email.com","amount":150.50,
                 "currency":"%s","externalReference":"PED-1"}""".formatted(currency);
    }

    private MvcResult create(String key, String currency) throws Exception {
        return perform(post("/api/payments").header("Idempotency-Key", key)
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

        perform(patch("/api/payments/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));

        perform(patch("/api/payments/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict());

        perform(get("/api/payments/{id}/audit", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[1].action").value("STATUS_UPDATED"));
    }

    @Test
    void missingIdempotencyKeyIs400AndUnknownPaymentIs404() throws Exception {
        perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(body("BRL")))
                .andExpect(status().isBadRequest());
        perform(get("/api/payments/{id}", 999999)).andExpect(status().isNotFound());
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
        perform(get("/api/payments?size=100")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.currency=='GBP')]", hasSize(1)));
    }

    @Test
    void rejectsMissingGarbageAndForgedTokens() throws Exception {
        mvc.perform(get("/api/payments")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/payments").header("Authorization", "Bearer lixo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/payments").header("Authorization",
                        "Bearer " + token("outro-segredo-qualquer-com-32-bytes-ou-mais")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiDocsIsPublic() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void healthEndpointIsPublicAndUp() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    }

    @Test
    void exposesBusinessAndResilienceMetricsToAuthenticatedScrapers() throws Exception {
        fx.stubFor(WireMock.get("/latest/CAD").willReturn(okJson("{\"rates\":{\"BRL\":4.0}}")));
        create("it-metrics-1", "CAD");

        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());

        String body = perform(get("/actuator/prometheus")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("payments_creations_total"), body);
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("application=\"payments\""), body);
        org.junit.jupiter.api.Assertions.assertTrue(body.contains("resilience4j_circuitbreaker_state"), body);
    }

    @Test
    void statusChangePublishesEventThroughOutbox() throws Exception {
        Integer id = JsonPath.read(create("it-event-1", "BRL").getResponse().getContentAsString(), "$.id");

        perform(patch("/api/payments/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJECTED\"}")).andExpect(status().isOk());

        // a fila é compartilhada com outros testes: consome até achar o evento deste pagamento
        Message found = null;
        long deadline = System.currentTimeMillis() + 20_000;
        while (found == null && System.currentTimeMillis() < deadline) {
            Message m = rabbitTemplate.receive(MessagingConfig.QUEUE, 2_000);
            if (m != null && new String(m.getBody()).contains("\"paymentId\":" + id + ",")) {
                found = m;
            }
        }

        org.junit.jupiter.api.Assertions.assertNotNull(found, "evento não chegou na fila");
        String json = new String(found.getBody());
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"previousStatus\":\"PENDING\""), json);
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("\"newStatus\":\"REJECTED\""), json);
        assertEquals("PagamentoStatusAlterado", found.getMessageProperties().getType());
        assertEquals("application/json", found.getMessageProperties().getContentType());
        org.junit.jupiter.api.Assertions.assertNotNull(found.getMessageProperties().getMessageId());
    }
}
