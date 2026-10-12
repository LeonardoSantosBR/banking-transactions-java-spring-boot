package leonardo.banking_transactions.dtos.transactions;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import leonardo.banking_transactions.enums.CurrencyEnum;
import leonardo.banking_transactions.enums.QrCodeTypeEnum;
import leonardo.banking_transactions.enums.TransactionChannelEnum;

public record TransactionCreateRequest(
        @NotBlank @Size(max = 100) String idempotencyKey,
        @NotNull UUID payerAccountId,
        @NotNull UUID payeeAccountId,
        @NotBlank @Size(max = 150) String pixKeyUsed,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal amount,
        QrCodeTypeEnum qrCodeType,
        @Size(max = 10000) String qrCodePayload,
        @Size(max = 100) String txid,
        CurrencyEnum currency,
        TransactionChannelEnum channel,
        @Size(max = 200) String description) {
}
