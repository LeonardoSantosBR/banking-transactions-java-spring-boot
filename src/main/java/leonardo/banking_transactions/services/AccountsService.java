package leonardo.banking_transactions.services;

import leonardo.banking_transactions.dtos.AccountCreateRequest;
import leonardo.banking_transactions.entities.AccountsEntity;
import leonardo.banking_transactions.exceptions.AccountNotFoundException;
import leonardo.banking_transactions.exceptions.AccountAlreadyRegisteredException;
import leonardo.banking_transactions.exceptions.UserNotFoundException;
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
    public AccountsEntity create(UUID userId, AccountCreateRequest request) {
        if (accountsRepository.existsByBranchAndAccountNumber(request.branch(), request.accountNumber())) {
            throw new AccountAlreadyRegisteredException(request.branch(), request.accountNumber());
        }

        AccountsEntity account = new AccountsEntity();
        account.setUser(usersRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException(userId)));
        account.setBranch(request.branch());
        account.setAccountNumber(request.accountNumber());
        return accountsRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<AccountsEntity> findByUser(UUID userId) {
        usersRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return accountsRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public AccountsEntity findById(UUID userId, UUID accountId) {
        return accountsRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Transactional
    public void delete(UUID userId, UUID accountId) {
        AccountsEntity account = findById(userId, accountId);
        accountsRepository.delete(account);
    }
}
