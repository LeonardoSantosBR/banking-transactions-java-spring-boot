CREATE TYPE outbox_event_status AS ENUM ('PENDING', 'PUBLISHED', 'FAILED');
 
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID                    NOT NULL,
    event_type      VARCHAR(100)            NOT NULL,
    payload         JSONB                   NOT NULL,
    status          outbox_event_status     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ             NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,
 
    CONSTRAINT fk_outbox_events_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id)
);
 
CREATE INDEX idx_outbox_events_status_created_at ON outbox_events (status, created_at) WHERE status = 'PENDING';