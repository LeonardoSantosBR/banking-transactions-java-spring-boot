package leonardo.banking_transactions.exceptions.accounts;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.api.ApiException;

public class AccountDataConflictExistingRecord extends ApiException {
    public AccountDataConflictExistingRecord() {
        super(HttpStatus.CONFLICT, "Account conflicts with an existing record");
    }
}
