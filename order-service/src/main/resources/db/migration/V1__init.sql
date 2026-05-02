CREATE TABLE IF NOT EXISTS orders (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    status           VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount     NUMERIC(10, 2) NOT NULL,
    shipping_address TEXT,
    tracking_number  VARCHAR(100),
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   BIGINT NOT NULL,
    product_name VARCHAR(255),
    unit_price   NUMERIC(10, 2) NOT NULL,
    quantity     INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
