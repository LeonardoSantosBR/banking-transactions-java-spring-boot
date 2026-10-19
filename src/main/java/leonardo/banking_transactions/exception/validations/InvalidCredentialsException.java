package leonardo.banking_transactions.exception.validations;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.api.ApiException;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid CPF or password");
    }
}
