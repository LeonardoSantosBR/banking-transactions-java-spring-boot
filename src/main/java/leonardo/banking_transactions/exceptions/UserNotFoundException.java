package leonardo.banking_transactions.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.ApiException;

public class UserNotFoundException extends ApiException {
     public UserNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "User not found:" + id);
    }
}
