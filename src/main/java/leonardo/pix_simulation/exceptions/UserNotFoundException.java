package leonardo.pix_simulation.exceptions;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import leonardo.pix_simulation.config.ApiException;

public class UserNotFoundException extends ApiException {
     public UserNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "User not found:" + id);
    }
}
