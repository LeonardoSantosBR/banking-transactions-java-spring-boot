package leonardo.banking_transactions.services;

import leonardo.banking_transactions.entities.AccountsEntity;
import leonardo.banking_transactions.repositories.AccountsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BalanceService {
    private final AccountsRepository accountsRepository;

    public BalanceService(AccountsRepository accountsRepository) {
        this.accountsRepository = accountsRepository;
    }

    @Transactional
    public boolean transfer(AccountsEntity payer, AccountsEntity payee, BigDecimal amount) {
        List<AccountsEntity> lockedAccounts = accountsRepository
                .findAllByIdInOrderByIdAsc(List.of(payer.getId(), payee.getId()));
        AccountsEntity lockedPayer = lockedAccounts.stream()
                .filter(account -> account.getId().equals(payer.getId()))
                .findFirst()
                .orElseThrow();
        AccountsEntity lockedPayee = lockedAccounts.stream()
                .filter(account -> account.getId().equals(payee.getId()))
                .findFirst()
                .orElseThrow();

        if (lockedPayer.getBalance().compareTo(amount) < 0)
            return false;
        lockedPayer.setBalance(lockedPayer.getBalance().subtract(amount));
        lockedPayee.setBalance(lockedPayee.getBalance().add(amount));
        return true;
    }
}
