package leonardo.banking_transactions.exceptions.pixKeys;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.api.ApiException;

public class PixKeyDataConflictExistingRecord extends ApiException {
    public PixKeyDataConflictExistingRecord() {
        super(HttpStatus.CONFLICT, "Pix key conflicts with an existing record");
    }
}
