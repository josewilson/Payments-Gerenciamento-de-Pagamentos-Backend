# Operação

## Configuração

Tudo por variável de ambiente (ver `.env.example`). Sem segredos no repositório. Senhas e `JWT_SECRET` não têm default.

## Banco

- PostgreSQL 16 com Flyway (`db/migration`), `ddl-auto: validate`, `open-in-view: false`.
- MongoDB 7 para auditoria.
- RabbitMQ 3.13 para eventos (ver `mensageria.md`); `RABBITMQ_USER`/`RABBITMQ_PASSWORD` sem default.

## Segurança

- `/api/payments/**` exige JWT Bearer (HS256, chave em `JWT_SECRET`, mínimo 32 bytes).
- Públicos: `/actuator/health/**`, Swagger e `/v3/api-docs/**`.

## Observabilidade

Actuator expõe `health` (liveness/readiness) e `info` (públicos), além de `metrics` e `prometheus` (exigem JWT, para o scraper usar Bearer token). Shutdown gracioso habilitado.

Métricas (Micrometer, tag `application`):

- negócio: `payments_creations_total`, `payments_status_changed_total{status}`
- resiliência: `resilience4j_circuitbreaker_*` e `resilience4j_retry_*` (binders registrados em `MetricsConfig`)
- JVM, HTTP e pool de conexões (padrão do Spring Boot)

## Arquitetura (ArchUnit)

`ArchitectureTest` falha o build se: controller for acessado por outra camada; repository ou client forem usados fora do service; `domain` depender de camadas superiores; houver ciclos entre pacotes; houver `@Autowired` em campo; exceções, controllers ou services estiverem fora do pacote próprio.

## Docker

`docker compose up -d --build` sobe postgres, mongo e a aplicação (imagem multi-stage, usuário não-root, healthchecks).

## CI

GitHub Actions: `./mvnw verify` (ITs com Testcontainers) a cada push/PR.
