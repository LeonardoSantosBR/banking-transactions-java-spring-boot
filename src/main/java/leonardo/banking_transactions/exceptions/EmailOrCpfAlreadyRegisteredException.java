package leonardo.banking_transactions.exceptions;

import org.springframework.http.HttpStatus;

import leonardo.banking_transactions.config.ApiException;

public class EmailOrCpfAlreadyRegisteredException extends ApiException {
     public EmailOrCpfAlreadyRegisteredException(String type, String emailOrCpf) {
        super(HttpStatus.CONFLICT, type + " already registered: " + emailOrCpf);
    }
}