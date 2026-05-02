CREATE TABLE IF NOT EXISTS carts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL UNIQUE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cart_items (
    id           BIGSERIAL PRIMARY KEY,
    cart_id      BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id   BIGINT NOT NULL,
    product_name VARCHAR(255),
    unit_price   NUMERIC(10, 2) NOT NULL,
    quantity     INTEGER NOT NULL DEFAULT 1,
    UNIQUE(cart_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_carts_user_id ON carts(user_id);
