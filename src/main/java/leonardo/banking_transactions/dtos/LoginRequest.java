package leonardo.banking_transactions.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String cpf, @NotBlank String password) {
}
