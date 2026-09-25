package leonardo.banking_transactions.exceptions;

import leonardo.banking_transactions.config.ApiException;
import org.springframework.http.HttpStatus;

public class PixKeyAlreadyRegisteredException extends ApiException {
    public PixKeyAlreadyRegisteredException(String value) {
        super(HttpStatus.CONFLICT, "Pix key already registered: " + value);
    }
}
