package leonardo.banking_transactions.dtos.login;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String cpf, @NotBlank String password) {
}
