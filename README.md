<h1 align="center">Payments API</h1>

<p align="center">
  API REST de pagamentos em Java 21 e Spring Boot 4: PostgreSQL para os dados, MongoDB para auditoria,
  cotação de câmbio via API externa, autenticação JWT e documentação OpenAPI.
</p>

<p align="center">
  <a href="https://github.com/josewilson/Payments-Gerenciamento-de-Pagamentos-Backend/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/josewilson/Payments-Gerenciamento-de-Pagamentos-Backend/actions/workflows/ci.yml/badge.svg"></a>
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java%2021-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white">
  <img alt="MongoDB" src="https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white">
  <img alt="Flyway" src="https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white">
  <img alt="Docker" src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">
  <img alt="Maven" src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white">
  <img alt="Swagger" src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black">
  <img alt="JUnit 5" src="https://img.shields.io/badge/JUnit%205-25A162?style=for-the-badge&logo=junit5&logoColor=white">
</p>

---

## Destaques

- **Idempotência** no `POST /api/payments` (header `Idempotency-Key`, índice único no banco, seguro contra corrida).
- **Máquina de status** (`PENDING` → `APPROVED`/`REJECTED`/`CANCELLED`) com lock otimista.
- **Cliente de câmbio robusto**: `RestClient` tipado com timeout, cache Caffeine (10 min), retry e circuit breaker (Resilience4j). Falha vira `503`, nunca `null` silencioso.
- **Valores monetários e cotações em `BigDecimal`.**
- **Migrações Flyway** (`ddl-auto: validate`), **sem segredos no repositório**.
- **Auditoria** de criação e mudança de status no MongoDB.
- **JWT** (HS256) protegendo `/api/payments/**`; health, Swagger e api-docs públicos.
- **Testes** com Postgres e Mongo reais (Testcontainers) e WireMock; **CI** no GitHub Actions.

## Executando

Pré-requisito: Docker.

```bash
cp .env.example .env     # troque as senhas e o JWT_SECRET
docker compose up -d --build
```

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`

Para rodar a aplicação pela IDE ou `./mvnw spring-boot:run`, suba só os bancos (`docker compose up -d postgres mongo`) e exporte as variáveis do `.env`.

### Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DB_USER`, `DB_PASSWORD` | sim | Credenciais do PostgreSQL |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | não | Padrão `localhost`, `5432`, `paymentsdb` |
| `MONGO_URI` | não | Padrão `mongodb://localhost:27017/payments_audit_db` |
| `JWT_SECRET` | sim | Chave HS256, mínimo 32 bytes (`openssl rand -base64 48`) |
| `EXCHANGE_RATE_URL` | não | Padrão `https://open.er-api.com/v6` |

## Autenticação

Todas as rotas `/api/payments/**` exigem `Authorization: Bearer <jwt>` assinado em HS256 com o `JWT_SECRET`. A API valida tokens; a emissão fica a cargo do seu provedor de identidade. Token de desenvolvimento:

```bash
b64() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }
H=$(printf '{"alg":"HS256","typ":"JWT"}' | b64)
P=$(printf '{"sub":"dev","exp":%s}' $(( $(date +%s) + 3600 )) | b64)
S=$(printf '%s.%s' "$H" "$P" | openssl dgst -binary -sha256 -hmac "$JWT_SECRET" | b64)
echo "$H.$P.$S"
```

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/payments` | Cria pagamento. Header `Idempotency-Key` obrigatório |
| GET | `/api/payments?status=&page=&size=` | Lista com paginação (máx. 100) e filtro |
| GET | `/api/payments/{id}` | Busca por id |
| PATCH | `/api/payments/{id}/status` | Altera o status (só a partir de `PENDING`) |
| GET | `/api/payments/{id}/audit` | Trilha de auditoria |

```bash
curl -X POST localhost:8080/api/payments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: pedido-123" \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Ana","customerEmail":"ana@email.com","amount":199.90,"currency":"USD","externalReference":"PEDIDO-123"}'
```

Repetir a requisição com a mesma `Idempotency-Key` devolve o mesmo pagamento sem criar outro.

### Erros

Formato único `{timestamp, status, error, message}` (`validationErrors` em 400).

| Código | Quando |
|---|---|
| 400 | Dados inválidos ou `Idempotency-Key` ausente |
| 401 | Token ausente, inválido ou expirado |
| 404 | Pagamento não encontrado |
| 409 | Transição de status inválida ou conflito de atualização concorrente |
| 503 | Cotação de câmbio indisponível (repita com a mesma `Idempotency-Key`) |

## Arquitetura

```text
controller → service → repository (JPA/PostgreSQL, Mongo)
                 └──→ client (câmbio: cache → retry → circuit breaker → RestClient)
```

Pacotes em `src/main/java/com/josewilson/payments`: `controller`, `service`, `repository`, `domain`, `dto`, `mapper`, `client`, `config`, `exception`. Especificações e decisões em [`specs/`](specs/README.md).

## Testes

```bash
./mvnw verify    # exige Docker (Testcontainers)
```

## Autor

<p align="center">
  Desenvolvido por <strong>José Wilson Alves de Souza</strong> para fins de estudo, prática em back-end Java e portfólio profissional.
</p>
