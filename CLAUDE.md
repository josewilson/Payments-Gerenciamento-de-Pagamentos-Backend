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
