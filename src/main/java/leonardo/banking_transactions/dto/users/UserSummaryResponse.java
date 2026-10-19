package leonardo.banking_transactions.dto.users;

import java.util.UUID;

import leonardo.banking_transactions.entity.UsersEntity;

public record UserSummaryResponse(UUID id, String name) {
    public static UserSummaryResponse from(UsersEntity user) {
        return new UserSummaryResponse(user.getId(), user.getName());
    }
}