package leonardo.banking_transactions.service;

import leonardo.banking_transactions.dto.accounts.AccountCreateRequest;
import leonardo.banking_transactions.entity.AccountsEntity;
import leonardo.banking_transactions.exception.accounts.AccountDataConflictExistingRecord;
import leonardo.banking_transactions.exception.accounts.AccountNotFoundException;
import leonardo.banking_transactions.exception.users.UserNotFoundException;
import leonardo.banking_transactions.repository.AccountsRepository;
import leonardo.banking_transactions.repository.UsersRepository;

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