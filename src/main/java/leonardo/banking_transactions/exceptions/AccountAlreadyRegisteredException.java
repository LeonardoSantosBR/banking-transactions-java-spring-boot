package leonardo.banking_transactions.exceptions;

import leonardo.banking_transactions.config.ApiException;
import org.springframework.http.HttpStatus;

public class AccountAlreadyRegisteredException extends ApiException {
    public AccountAlreadyRegisteredException(String branch, String accountNumber) {
        super(HttpStatus.CONFLICT,
                "Account already registered: branch=" + branch + ", accountNumber=" + accountNumber);
    }
}
