package leonardo.banking_transactions.exceptions;

import leonardo.banking_transactions.config.ApiException;
import org.springframework.http.HttpStatus;

public class PixKeyDataConflictExistingRecord extends ApiException {
    public PixKeyDataConflictExistingRecord() {
        super(HttpStatus.CONFLICT, "Pix key conflicts with an existing record");
    }
}
