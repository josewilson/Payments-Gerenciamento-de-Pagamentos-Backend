# Operação

## Configuração

Tudo por variável de ambiente (ver `.env.example`). Sem segredos no repositório. Senhas e `JWT_SECRET` não têm default.

## Banco

- PostgreSQL 16 com Flyway (`db/migration`), `ddl-auto: validate`, `open-in-view: false`.
- MongoDB 7 para auditoria.

## Segurança

- `/api/payments/**` exige JWT Bearer (HS256, chave em `JWT_SECRET`, mínimo 32 bytes).
- Públicos: `/actuator/health/**`, Swagger e `/v3/api-docs/**`.

## Observabilidade

Actuator expõe `health` (liveness/readiness) e `info`. Shutdown gracioso habilitado.

## Docker

`docker compose up -d --build` sobe postgres, mongo e a aplicação (imagem multi-stage, usuário não-root, healthchecks).

## CI

GitHub Actions: `./mvnw verify` (ITs com Testcontainers) a cada push/PR.
