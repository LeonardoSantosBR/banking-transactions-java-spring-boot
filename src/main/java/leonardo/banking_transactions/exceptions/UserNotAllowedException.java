package leonardo.banking_transactions.exceptions;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.ApiException;

public class UserNotAllowedException extends ApiException {
    public UserNotAllowedException() {
        super(HttpStatus.FORBIDDEN, "User is not authorized to access this resource");
    }
}
