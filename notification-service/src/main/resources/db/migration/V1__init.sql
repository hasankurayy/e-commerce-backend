CREATE TABLE IF NOT EXISTS notification_logs (
    id            BIGSERIAL PRIMARY KEY,
    event_type    VARCHAR(50) NOT NULL,
    order_id      BIGINT,
    user_id       BIGINT,
    recipient     VARCHAR(255),
    subject       VARCHAR(500),
    sent          BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
