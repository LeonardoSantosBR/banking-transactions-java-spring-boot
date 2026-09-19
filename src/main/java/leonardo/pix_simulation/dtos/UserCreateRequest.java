package leonardo.pix_simulation.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
                @NotBlank @Size(max = 150) String name,
                @NotBlank @Size(max = 11) String cpf,
                @NotBlank @Email @Size(max = 150) String email,
                @NotBlank @Size(min = 6, max = 100) String password) {
}
