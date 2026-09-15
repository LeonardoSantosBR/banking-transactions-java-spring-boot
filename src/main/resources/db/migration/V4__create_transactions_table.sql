CREATE TYPE transaction_status AS ENUM ('PROCESSING', 'SETTLED', 'REJECTED', 'REFUNDED');
 
CREATE TABLE transactions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    end_to_end_id       VARCHAR(50)         NOT NULL,
    idempotency_key     VARCHAR(100)        NOT NULL,
    payer_account_id    UUID                NOT NULL,
    payee_account_id    UUID                NOT NULL,
    pix_key_used        VARCHAR(150)        NOT NULL,
    amount              DECIMAL(19,4)       NOT NULL,
    status              transaction_status  NOT NULL DEFAULT 'PROCESSING',
    description         VARCHAR(200),
    created_at          TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ         NOT NULL DEFAULT now(),
 
    CONSTRAINT fk_transactions_payer_account FOREIGN KEY (payer_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transactions_payee_account FOREIGN KEY (payee_account_id) REFERENCES accounts (id),
    CONSTRAINT uk_transactions_end_to_end_id UNIQUE (end_to_end_id),
    CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transactions_different_accounts CHECK (payer_account_id <> payee_account_id)
);
 
CREATE INDEX idx_transactions_payer_account_id ON transactions (payer_account_id);
CREATE INDEX idx_transactions_payee_account_id ON transactions (payee_account_id);
CREATE INDEX idx_transactions_status ON transactions (status);