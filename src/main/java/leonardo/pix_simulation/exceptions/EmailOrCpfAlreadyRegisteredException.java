package leonardo.pix_simulation.exceptions;

import org.springframework.http.HttpStatus;

import leonardo.pix_simulation.config.ApiException;

public class EmailOrCpfAlreadyRegisteredException extends ApiException {
     public EmailOrCpfAlreadyRegisteredException(String type, String emailOrCpf) {
        super(HttpStatus.CONFLICT, type + " already registered: " + emailOrCpf);
    }
}