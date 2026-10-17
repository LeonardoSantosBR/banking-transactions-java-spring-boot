ALTER TABLE transactions ADD COLUMN refund_of_transaction_id UUID;
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_refund_of
    FOREIGN KEY (refund_of_transaction_id) REFERENCES transactions(id);
CREATE UNIQUE INDEX uk_transactions_refund_of ON transactions (refund_of_transaction_id)
    WHERE refund_of_transaction_id IS NOT NULL;
