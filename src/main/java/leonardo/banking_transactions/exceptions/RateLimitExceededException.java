package leonardo.banking_transactions.exceptions;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.api.ApiException;

public class RateLimitExceededException extends ApiException {
    public RateLimitExceededException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Try again later.");
    }
}
