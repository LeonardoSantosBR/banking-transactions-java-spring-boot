package leonardo.banking_transactions.dtos.signin;

import jakarta.validation.constraints.NotBlank;

public record SigninRequest(@NotBlank String cpf, @NotBlank String password) {
}
