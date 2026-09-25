package leonardo.banking_transactions.exceptions;

import leonardo.banking_transactions.config.ApiException;
import org.springframework.http.HttpStatus;
import java.util.UUID;

public class PixKeyNotFoundException extends ApiException {
    public PixKeyNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Pix key not found: " + id);
    }
}