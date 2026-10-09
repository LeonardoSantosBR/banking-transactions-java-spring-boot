package leonardo.banking_transactions.services;

import leonardo.banking_transactions.dtos.accounts.AccountCreateRequest;
import leonardo.banking_transactions.entities.AccountsEntity;
import leonardo.banking_transactions.exceptions.accounts.AccountDataConflictExistingRecord;
import leonardo.banking_transactions.exceptions.accounts.AccountNotFoundException;
import leonardo.banking_transactions.exceptions.users.UserNotFoundException;
import leonardo.banking_transactions.repositories.AccountsRepository;
import leonardo.banking_transactions.repositories.UsersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AccountsService {
    private final AccountsRepository accountsRepository;
    private final UsersRepository usersRepository;

    public AccountsService(AccountsRepository accountsRepository, UsersRepository usersRepository) {
        this.accountsRepository = accountsRepository;
        this.usersRepository = usersRepository;
    }

    @Transactional
    public AccountsEntity create(UUID authenticatedUserId, AccountCreateRequest request) {
        if (accountsRepository.existsByBranchAndAccountNumber(request.branch(), request.accountNumber()))
            throw new AccountDataConflictExistingRecord();
        AccountsEntity account = new AccountsEntity();
        account.setUser(usersRepository.findByIdAndDeletedAtIsNull(authenticatedUserId)
                .orElseThrow(() -> new UserNotFoundException(authenticatedUserId)));
        account.setBranch(request.branch());
        account.setAccountNumber(request.accountNumber());
        return accountsRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<AccountsEntity> findByUser(UUID authenticatedUserId) {
        usersRepository.findByIdAndDeletedAtIsNull(authenticatedUserId)
                .orElseThrow(() -> new UserNotFoundException(authenticatedUserId));
        return accountsRepository.findByUserId(authenticatedUserId);
    }

    @Transactional(readOnly = true)
    public AccountsEntity findById(UUID authenticatedUserId, UUID accountId) {
        return accountsRepository.findByIdAndUserId(accountId, authenticatedUserId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional
    public void delete(UUID authenticatedUserId, UUID accountId) {
        AccountsEntity account = findById(authenticatedUserId, accountId);
        accountsRepository.delete(account);
    }
}