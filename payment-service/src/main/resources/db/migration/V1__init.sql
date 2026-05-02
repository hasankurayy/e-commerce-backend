CREATE TABLE IF NOT EXISTS payments (
    id                      BIGSERIAL PRIMARY KEY,
    order_id                BIGINT NOT NULL,
    user_id                 BIGINT NOT NULL,
    amount                  NUMERIC(10, 2) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    iyzico_payment_id       VARCHAR(100),
    iyzico_conversation_id  VARCHAR(100),
    failure_reason          TEXT,
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_conversation_id ON payments(iyzico_conversation_id);
