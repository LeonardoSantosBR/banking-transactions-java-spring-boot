package leonardo.banking_transactions.exceptions;

import leonardo.banking_transactions.config.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidTransactionException extends ApiException {
    public InvalidTransactionException(String message) { super(HttpStatus.UNPROCESSABLE_CONTENT, message); }
}
