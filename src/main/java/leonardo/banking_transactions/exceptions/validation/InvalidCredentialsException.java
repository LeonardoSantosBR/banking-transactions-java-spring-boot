package leonardo.banking_transactions.exceptions.validation;

import org.springframework.http.HttpStatus;
import leonardo.banking_transactions.config.ApiException;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid CPF or password");
    }
}
