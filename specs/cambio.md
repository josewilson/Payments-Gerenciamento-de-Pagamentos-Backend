# Câmbio

Cotação da moeda do pagamento para BRL, via API externa (`open.er-api.com`, sem chave; URL configurável).

- Cliente: `RestClient`, timeout de conexão 2s e de leitura 3s (configuráveis).
- Resposta mapeada em DTO tipado `ExchangeRateResponse`; cotação em `BigDecimal`.
- `BRL` → 1 sem chamada externa.
- Falha (timeout, 5xx, resposta sem BRL) → `CotacaoIndisponivelException` → HTTP 503. Nunca `null`.
- Cache `cotacoes` (Caffeine), expira em 10 min; falhas não são cacheadas.
- Resilience4j: `@Retry` (3 tentativas) e `@CircuitBreaker` no cliente. Cache é a camada mais externa, para que hits não diluam a taxa de falha do circuito.
- Efeito: criar pagamento com câmbio fora do ar retorna 503 depois de persistir; o cliente repete com a mesma `Idempotency-Key` e recebe o pagamento existente.
