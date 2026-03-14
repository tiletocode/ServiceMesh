CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)   NOT NULL,
    description TEXT           NOT NULL,
    sales_price NUMERIC(15, 2) NOT NULL,
    category    VARCHAR(30)    NOT NULL,
    image_url   VARCHAR(500),
    is_on_sale  BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE TABLE skus (
    id             BIGSERIAL PRIMARY KEY,
    product_id     BIGINT       NOT NULL REFERENCES products (id),
    sku_code       VARCHAR(100) NOT NULL UNIQUE,
    option_name    VARCHAR(255) NOT NULL,
    stock_quantity INT          NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_skus_product_id ON skus (product_id);
CREATE INDEX idx_skus_sku_code ON skus (sku_code);
