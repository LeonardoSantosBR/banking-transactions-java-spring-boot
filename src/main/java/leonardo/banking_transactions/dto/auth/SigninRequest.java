package leonardo.banking_transactions.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record SigninRequest(@NotBlank String cpf, @NotBlank String password) {
}
