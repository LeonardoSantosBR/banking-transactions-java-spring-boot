package leonardo.banking_transactions.exceptions.transactions;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.api.ApiException;

import java.util.UUID;

public class TransactionNotFoundException extends ApiException {
    public TransactionNotFoundException(UUID id) { super(HttpStatus.NOT_FOUND, "Transaction not found: " + id); }
}
