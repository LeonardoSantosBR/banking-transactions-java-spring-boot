package leonardo.banking_transactions.exceptions.transactions;

import leonardo.banking_transactions.config.ApiException;
import org.springframework.http.HttpStatus;

public class TransactionDataConflictExistingRecord extends ApiException {
    public TransactionDataConflictExistingRecord() {
        super(HttpStatus.CONFLICT, "Request conflicts with an existing transaction");
    }
}