package leonardo.banking_transactions.exceptions;

import leonardo.banking_transactions.config.ApiException;
import org.springframework.http.HttpStatus;

public class LoginRateLimitExceededException extends ApiException {
    public LoginRateLimitExceededException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Try again later.");
    }
}
