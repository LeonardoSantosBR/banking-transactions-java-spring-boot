package leonardo.banking_transactions.dtos;

import leonardo.banking_transactions.entities.UsersEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String name, String cpf, String email, String phone,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static UserResponse from(UsersEntity user) {
        return new UserResponse(user.getId(), user.getName(), user.getCpf(), user.getEmail(), user.getPhone(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
