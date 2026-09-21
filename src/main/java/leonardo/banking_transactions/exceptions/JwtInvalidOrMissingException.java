package leonardo.banking_transactions.exceptions;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.ApiException;

public class JwtInvalidOrMissingException extends ApiException {
    public JwtInvalidOrMissingException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid or missing JWT");
    }
}
