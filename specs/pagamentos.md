# Pagamentos

## Criação — `POST /api/payments`

- Header obrigatório `Idempotency-Key` (string, até 100 caracteres). Ausente → 400.
- Mesma chave repetida → mesma resposta do pagamento original, sem criar outro (garantido por índice único no banco; corrida entre duas requisições resolve pelo índice).
- Moeda: 3 letras (ISO 4217), normalizada para maiúsculas. Valor mínimo 0,01.
- Resposta 201 com `exchangeRateToBrl` (`BigDecimal`).

## Status

Estados: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`.

| De | Para permitido |
|---|---|
| `PENDING` | `APPROVED`, `REJECTED`, `CANCELLED` |
| `APPROVED`, `REJECTED`, `CANCELLED` | nenhum (terminais) |

Transição inválida → 409. Atualizações concorrentes protegidas por lock otimista (`version`); conflito → 409.

## Auditoria (MongoDB)

Toda criação (`CREATED`) e mudança de status (`STATUS_UPDATED`) grava um documento em `payment_audit`.
Consulta: `GET /api/payments/{id}/audit`.

## Erros

Formato único: `timestamp`, `status`, `error`, `message` (e `validationErrors` em 400). Erro 500 nunca expõe a mensagem da exceção.
