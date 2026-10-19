package leonardo.banking_transactions.exceptions.pixKeys;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.api.ApiException;

import java.util.UUID;

public class PixKeyNotFoundException extends ApiException {
    public PixKeyNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Pix key not found: " + id);
    }
}