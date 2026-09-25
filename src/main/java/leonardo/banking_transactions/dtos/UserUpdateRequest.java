package leonardo.banking_transactions.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 11) String cpf,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @Size(min = 6, max = 100) String password) {
}
