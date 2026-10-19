package leonardo.banking_transactions.dto.accounts;

import java.math.BigDecimal;
import java.util.UUID;

import leonardo.banking_transactions.entity.AccountsEntity;

public record AccountResponse(
        UUID id,
        UUID userId,
        String branch,
        String accountNumber,
        BigDecimal balance,
        Long version) {

    public static AccountResponse from(AccountsEntity account) {
        return new AccountResponse(
                account.getId(),
                account.getUser().getId(),
                account.getBranch(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getVersion());
    }
}
