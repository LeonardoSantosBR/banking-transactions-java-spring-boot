package leonardo.banking_transactions.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccountCreateRequest(
        @NotBlank @Size(max = 10) String branch,
        @NotBlank @Size(max = 20) String accountNumber
) {
}
