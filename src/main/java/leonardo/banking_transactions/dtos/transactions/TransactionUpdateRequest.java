package leonardo.banking_transactions.dtos.transactions;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import leonardo.banking_transactions.enums.TransactionStatusEnum;

public record TransactionUpdateRequest(
        @NotNull TransactionStatusEnum status,
        @Size(max = 200) String description) {
}
