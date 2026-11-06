ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100);

-- NULL para pagamentos antigos; o indice unico ignora NULLs.
CREATE UNIQUE INDEX uq_payments_idempotency_key ON payments (idempotency_key);
