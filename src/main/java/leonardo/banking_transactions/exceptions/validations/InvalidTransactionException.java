package leonardo.banking_transactions.exceptions.validations;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.api.ApiException;

public class InvalidTransactionException extends ApiException {
    public InvalidTransactionException(String message) { super(HttpStatus.UNPROCESSABLE_CONTENT, message); }
}
