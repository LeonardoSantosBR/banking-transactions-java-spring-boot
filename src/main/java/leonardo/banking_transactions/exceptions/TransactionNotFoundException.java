package leonardo.banking_transactions.exceptions;

import leonardo.banking_transactions.config.ApiException;
import org.springframework.http.HttpStatus;
import java.util.UUID;

public class TransactionNotFoundException extends ApiException {
    public TransactionNotFoundException(UUID id) { super(HttpStatus.NOT_FOUND, "Transaction not found: " + id); }
}
