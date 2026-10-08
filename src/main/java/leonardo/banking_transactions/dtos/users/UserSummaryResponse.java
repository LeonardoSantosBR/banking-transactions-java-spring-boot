package leonardo.banking_transactions.dtos.users;

import leonardo.banking_transactions.entities.UsersEntity;

import java.util.UUID;

public record UserSummaryResponse(UUID id, String name) {
    public static UserSummaryResponse from(UsersEntity user) {
        return new UserSummaryResponse(user.getId(), user.getName());
    }
}