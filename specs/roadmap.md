# Roadmap

Um passo = um commit. Marque `[x]` ao concluir.

- [x] 1. Docker Compose — `chore(docker): ambiente completo com postgres e mongo`
- [x] 2. Flyway — `feat(db): migracoes com Flyway`
- [x] 3. Cliente de câmbio tipado — `refactor(cambio): cliente tipado com timeout`
- [x] 4. Cache da cotação — `feat(cache): cache de cotacao com expiracao`
- [x] 5. Resiliência — `feat(resiliencia): retry e circuit breaker`
- [x] 6. Idempotência do POST — `feat(api): idempotencia na criacao de pagamento`
- [x] 7. Transições de status + lock otimista — `feat(dominio): transicoes de status validas`
- [x] 8. Testes (serviço + IT com Testcontainers) — `test: servico e integracao com bancos reais`
- [x] 9. Autenticação JWT — `feat(seguranca): API protegida por JWT`
- [x] 10. Actuator + CI — `ci: build e testes`

Fora de escopo: publicar `PagamentoStatusAlterado` em fila (só quando Pix e pedido tiverem mensageria).
