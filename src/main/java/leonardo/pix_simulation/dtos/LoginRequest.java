package leonardo.pix_simulation.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String cpf, @NotBlank String password) {
}
