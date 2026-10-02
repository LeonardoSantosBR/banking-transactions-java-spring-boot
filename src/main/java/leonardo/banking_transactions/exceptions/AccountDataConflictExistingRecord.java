package leonardo.banking_transactions.exceptions;

import leonardo.banking_transactions.config.ApiException;
import org.springframework.http.HttpStatus;

public class AccountDataConflictExistingRecord extends ApiException {
    public AccountDataConflictExistingRecord() {
        super(HttpStatus.CONFLICT,
                "Account conflicts with an existing record");
    }
}
