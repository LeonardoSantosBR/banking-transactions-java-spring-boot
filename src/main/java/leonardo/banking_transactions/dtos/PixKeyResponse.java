package leonardo.banking_transactions.dtos;

import leonardo.banking_transactions.entities.PixKeysEntity;
import leonardo.banking_transactions.enums.PixKeyTypeEnum;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PixKeyResponse(
        UUID id,
        UUID accountId,
        PixKeyTypeEnum keyType,
        String keyValue,
        boolean active, OffsetDateTime createdAt) {

    public static PixKeyResponse from(PixKeysEntity key) {
        return new PixKeyResponse(
                key.getId(), key.getAccount().getId(), key.getKeyType(),
                key.getKeyValue(), key.isActive(), key.getCreatedAt());
    }
}
