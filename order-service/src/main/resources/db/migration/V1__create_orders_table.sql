CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    member_id        BIGINT         NOT NULL,
    order_number     VARCHAR(50)    NOT NULL UNIQUE,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_amount     NUMERIC(15, 2) NOT NULL DEFAULT 0,
    shipping_address VARCHAR(500)   NOT NULL,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE order_lines (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT         NOT NULL REFERENCES orders (id),
    sku_id       BIGINT         NOT NULL,
    sku_code     VARCHAR(100)   NOT NULL,
    product_name VARCHAR(255)   NOT NULL,
    quantity     INT            NOT NULL CHECK (quantity > 0),
    order_price  NUMERIC(15, 2) NOT NULL
);

CREATE INDEX idx_orders_member_id ON orders (member_id);
CREATE INDEX idx_orders_order_number ON orders (order_number);
CREATE INDEX idx_order_lines_order_id ON order_lines (order_id);
