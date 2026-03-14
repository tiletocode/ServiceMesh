CREATE TABLE payments (
    id                BIGSERIAL PRIMARY KEY,
    order_id          BIGINT         NOT NULL,
    order_number      VARCHAR(50)    NOT NULL,
    member_id         BIGINT         NOT NULL,
    amount            NUMERIC(15, 2) NOT NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'REQUESTED',
    failure_reason    VARCHAR(500),
    pg_transaction_id VARCHAR(200),
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_order_number ON payments (order_number);
