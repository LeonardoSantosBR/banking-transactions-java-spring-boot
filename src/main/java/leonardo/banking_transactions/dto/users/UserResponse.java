package leonardo.banking_transactions.dto.users;

import java.time.OffsetDateTime;
import java.util.UUID;

import leonardo.banking_transactions.entity.UsersEntity;

public record UserResponse(
        UUID id,
        String name,
        String cpf,
        String email,
        String phone,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static UserResponse from(UsersEntity user) {
        return new UserResponse(user.getId(), user.getName(), user.getCpf(), user.getEmail(), user.getPhone(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
