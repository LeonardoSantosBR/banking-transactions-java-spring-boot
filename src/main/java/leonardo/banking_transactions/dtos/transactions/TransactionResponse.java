package leonardo.banking_transactions.dtos.transactions;

import leonardo.banking_transactions.entities.TransactionsEntity;
import leonardo.banking_transactions.enums.TransactionStatusEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        String channel,
        TransactionStatusEnum status,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static TransactionResponse from(TransactionsEntity t) {
        return new TransactionResponse(t.getId(),
                t.getAmount(), t.getCurrency(),
                t.getChannel(), t.getStatus(), t.getDescription(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
