ALTER TABLE users ADD COLUMN phone VARCHAR(20);

ALTER TABLE transactions
    ADD COLUMN qr_code_type VARCHAR(20),
    ADD COLUMN qr_code_payload TEXT,
    ADD COLUMN txid VARCHAR(100),
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    ADD COLUMN channel VARCHAR(30);

CREATE INDEX idx_transactions_txid ON transactions (txid);
