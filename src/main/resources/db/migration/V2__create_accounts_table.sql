CREATE TABLE accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    branch          VARCHAR(10)     NOT NULL,
    account_number  VARCHAR(20)     NOT NULL,
    balance         DECIMAL(19,4)   NOT NULL DEFAULT 0,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
 
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_accounts_branch_number UNIQUE (branch, account_number),
    CONSTRAINT ck_accounts_balance_non_negative CHECK (balance >= 0)
);
 
CREATE INDEX idx_accounts_user_id ON accounts (user_id);