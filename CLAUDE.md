# Payments API

API REST de pagamentos: Spring Boot 4.0.4, Java 21, PostgreSQL (dados) + MongoDB (auditoria).
Especificações e decisões em `specs/` (comece por `specs/README.md`).

## Comandos

```bash
cp .env.example .env && docker compose up -d --build   # stack completa
docker compose up -d postgres mongo                    # só os bancos (rodar app pela IDE/mvn)
./mvnw test                                            # testes (ITs usam Testcontainers: exige Docker)
./mvnw spring-boot:run
```

Swagger: `http://localhost:8080/swagger-ui/index.html` · Health: `/actuator/health`

## Arquitetura

`controller` → `service` → `repository` (JPA/Postgres e Mongo). `client` isola APIs externas (câmbio).
`dto` na borda, `mapper` converte, `exception` tem o `GlobalExceptionHandler` único.

## Convenções

- Dinheiro e cotação: sempre `BigDecimal`, nunca `Double`.
- Schema só muda por migração Flyway (`src/main/resources/db/migration`); `ddl-auto: validate`.
- Nenhum segredo no repositório: tudo via variável de ambiente (`.env.example` documenta).
- Chamadas externas: cliente tipado, com timeout, e falha vira exceção de domínio (nunca `null` silencioso).
- Commits em português, Conventional Commits (`feat(escopo): ...`), um passo do roadmap por commit.
- Mudou comportamento? Atualize a spec correspondente em `specs/`.

## Armadilhas do Spring Boot 4 (já encontradas aqui)

- URI do Mongo é `spring.mongodb.uri` (a antiga `spring.data.mongodb.uri` é ignorada).
- Jackson 3: `tools.jackson.databind.ObjectMapper` (anotações continuam em `com.fasterxml.jackson.annotation`).
- Slice `@WebMvcTest` com segurança: `@Import(SecurityConfig.class)` + `@MockitoBean JwtDecoder` + `jwt()` nas requisições.
- Testes de integração se chamam `*Test` (surefire não executa `*IT`) e exigem Docker.
- `ExchangeRateClient` fica sob cache → retry → circuit breaker; o atalho BRL fica no `PaymentService`, fora do circuito.
- Métricas: `/actuator/prometheus` exige JWT. Não nomeie contador terminando em `.created` (sufixo reservado do Prometheus: o contador some). Binders do Resilience4j são registrados à mão em `MetricsConfig`.
- Regras de arquitetura em `ArchitectureTest`: camada nova ou dependência fora do padrão quebra o build.
