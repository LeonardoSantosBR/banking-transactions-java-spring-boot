package leonardo.banking_transactions.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCreateRequest(
        @NotBlank @Size(max = 100) String idempotencyKey,
        @NotNull UUID payerAccountId,
        @NotNull UUID payeeAccountId,
        @NotBlank @Size(max = 150) String pixKeyUsed,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal amount,
        @Size(max = 20) String qrCodeType,
        String qrCodePayload,
        @Size(max = 100) String txid,
        @Size(max = 3) String currency,
        @Size(max = 30) String channel,
        @Size(max = 200) String description) {
}
