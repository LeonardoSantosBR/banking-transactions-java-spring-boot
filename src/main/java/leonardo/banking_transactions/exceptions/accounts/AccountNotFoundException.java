package leonardo.banking_transactions.exceptions.accounts;

import leonardo.banking_transactions.config.ApiException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AccountNotFoundException extends ApiException {
    public AccountNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Account not found: " + id);
    }
}
