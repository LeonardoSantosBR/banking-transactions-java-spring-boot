package leonardo.banking_transactions.exception.validations;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.api.ApiException;

public class InvalidJwtOrMissingException extends ApiException {
    public InvalidJwtOrMissingException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid or missing JWT");
    }
}
