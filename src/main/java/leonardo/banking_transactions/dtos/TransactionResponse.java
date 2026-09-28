package leonardo.banking_transactions.dtos;

import leonardo.banking_transactions.entities.TransactionsEntity;
import leonardo.banking_transactions.enums.TransactionStatusEnum;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        String endToEndId,
        String idempotencyKey,
        UUID payerAccountId,
        UUID payeeAccountId,
        String pixKeyUsed,
        String qrCodeType,
        String qrCodePayload,
        String txid,
        BigDecimal amount,
        String currency,
        String channel,
        TransactionStatusEnum status,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static TransactionResponse from(TransactionsEntity t) {
        return new TransactionResponse(t.getId(), t.getEndToEndId(), t.getIdempotencyKey(),
                t.getPayerAccount().getId(), t.getPayeeAccount().getId(), t.getPixKeyUsed(),
                t.getQrCodeType(), t.getQrCodePayload(), t.getTxid(), t.getAmount(), t.getCurrency(),
                t.getChannel(), t.getStatus(), t.getDescription(), t.getCreatedAt(), t.getUpdatedAt());
    }
}
