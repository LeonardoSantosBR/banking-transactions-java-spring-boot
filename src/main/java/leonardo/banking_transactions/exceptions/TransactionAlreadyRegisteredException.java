package leonardo.banking_transactions.exceptions;

import leonardo.banking_transactions.config.ApiException;
import org.springframework.http.HttpStatus;

public class TransactionAlreadyRegisteredException extends ApiException {
    public TransactionAlreadyRegisteredException(String key) { super(HttpStatus.CONFLICT, "Transaction idempotency key already registered: " + key); }
}
