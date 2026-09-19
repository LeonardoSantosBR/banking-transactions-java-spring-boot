package leonardo.pix_simulation.exceptions;

import org.springframework.http.HttpStatus;
import leonardo.pix_simulation.config.ApiException;

public class InvalidCredentialsException extends ApiException {
    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid CPF or password");
    }
}
