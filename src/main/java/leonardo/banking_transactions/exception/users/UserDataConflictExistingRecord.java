package leonardo.banking_transactions.exception.users;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.api.ApiException;

public class UserDataConflictExistingRecord extends ApiException {
     public UserDataConflictExistingRecord() {
        super(HttpStatus.CONFLICT, "User data conflicts with an existing record");
    }
}