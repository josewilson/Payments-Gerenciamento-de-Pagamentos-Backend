# Mensageria

Evento **`PagamentoStatusAlterado`** (classe `PaymentStatusChangedEvent`) publicado no RabbitMQ a cada mudança de status.

## Garantia

Transactional outbox, entrega **at-least-once**:

1. `PATCH /api/payments/{id}/status` grava o novo status **e** uma linha em `outbox_event` na mesma transação do PostgreSQL. Não existe status alterado sem evento, nem evento sem status alterado.
2. `OutboxRelay` (a cada 1s, `outbox.poll-interval`) lê os pendentes com `FOR UPDATE SKIP LOCKED` e publica com confirmação do broker (`publisher-confirm-type: simple`).
3. Só depois da confirmação marca `published_at`. Broker fora do ar: o lote para, os eventos continuam pendentes e saem quando ele voltar.
4. Várias instâncias da API podem rodar o relay ao mesmo tempo sem publicar em duplicidade pelo lock.

Duplicata ainda é possível (a API cai entre a confirmação e o commit): **o consumidor deve descartar pelo `eventId`**.

## Topologia

| Item | Valor |
|---|---|
| Exchange | `payments.events` (topic, durável) |
| Routing key | `payment.status.changed` |
| Fila | `payments.status-changed` (durável, criada pela API para não perder eventos sem consumidor) |
| Headers | `type=PagamentoStatusAlterado`, `messageId=<eventId>`, `content-type=application/json` |

## Payload

```json
{
  "eventId": "6f1c0a52-...",
  "paymentId": 1,
  "previousStatus": "PENDING",
  "newStatus": "APPROVED",
  "amount": 199.90,
  "currency": "USD",
  "occurredAt": "2026-11-06T19:45:00.123Z"
}
```

## Observabilidade

`payments_events_published_total` e `payments_events_publish_failed_total`. Evento pendente se acumulando = broker indisponível.

## Limites conhecidos

- Eventos publicados não são apagados de `outbox_event` (falta job de limpeza).
- Sem fila de mensagens mortas: o consumidor define a política de reprocessamento.
- Só a mudança de status gera evento; a criação do pagamento não.
