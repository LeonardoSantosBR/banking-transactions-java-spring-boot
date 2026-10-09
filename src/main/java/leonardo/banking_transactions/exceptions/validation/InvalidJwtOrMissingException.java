package leonardo.banking_transactions.exceptions.validation;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.ApiException;

public class InvalidJwtOrMissingException extends ApiException {
    public InvalidJwtOrMissingException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid or missing JWT");
    }
}
