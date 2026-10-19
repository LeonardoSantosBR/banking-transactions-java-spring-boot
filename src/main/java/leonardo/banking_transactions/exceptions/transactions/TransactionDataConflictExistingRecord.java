package leonardo.banking_transactions.exceptions.transactions;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.api.ApiException;

public class TransactionDataConflictExistingRecord extends ApiException {
    public TransactionDataConflictExistingRecord() {
        super(HttpStatus.CONFLICT, "Request conflicts with an existing transaction");
    }
}