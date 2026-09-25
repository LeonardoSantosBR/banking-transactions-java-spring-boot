package leonardo.banking_transactions.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import leonardo.banking_transactions.enums.PixKeyTypeEnum;

public record PixKeyCreateRequest(
                @NotNull PixKeyTypeEnum keyType,
                @NotBlank @Size(max = 150) String keyValue) {
}
