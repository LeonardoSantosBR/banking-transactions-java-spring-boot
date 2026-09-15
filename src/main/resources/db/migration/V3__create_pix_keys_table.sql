CREATE TYPE pix_key_type AS ENUM ('CPF', 'EMAIL', 'PHONE', 'RANDOM');
 
CREATE TABLE pix_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID            NOT NULL,
    key_type        pix_key_type    NOT NULL,
    key_value       VARCHAR(150)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
 
    CONSTRAINT fk_pix_keys_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT uk_pix_keys_key_value UNIQUE (key_value)
);
 
CREATE INDEX idx_pix_keys_account_id ON pix_keys (account_id);
CREATE INDEX idx_pix_keys_key_value_active ON pix_keys (key_value) WHERE active = true;