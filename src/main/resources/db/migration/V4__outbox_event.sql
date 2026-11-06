-- Transactional outbox: o evento nasce na mesma transacao da mudanca de status.
CREATE TABLE outbox_event (
    id           UUID         PRIMARY KEY,
    aggregate_id BIGINT       NOT NULL,
    type         VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    published_at TIMESTAMP
);

CREATE INDEX idx_outbox_pending ON outbox_event (created_at) WHERE published_at IS NULL;
