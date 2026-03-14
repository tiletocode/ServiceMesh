CREATE TABLE shipments (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT       NOT NULL,
    order_number     VARCHAR(50)  NOT NULL,
    member_id        BIGINT       NOT NULL,
    shipping_address VARCHAR(500) NOT NULL,
    tracking_number  VARCHAR(100) UNIQUE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PREPARING',
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE shipment_items (
    id           BIGSERIAL PRIMARY KEY,
    shipment_id  BIGINT       NOT NULL REFERENCES shipments (id),
    sku_id       BIGINT       NOT NULL,
    sku_code     VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity     INT          NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_shipments_order_id ON shipments (order_id);
CREATE INDEX idx_shipments_tracking_number ON shipments (tracking_number);
CREATE INDEX idx_shipments_member_id ON shipments (member_id);
