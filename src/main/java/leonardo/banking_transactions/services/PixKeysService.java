package leonardo.banking_transactions.services;

import leonardo.banking_transactions.dtos.pixKeys.PixKeyCreateRequest;
import leonardo.banking_transactions.dtos.pixKeys.PixKeyUpdateRequest;
import leonardo.banking_transactions.entities.PixKeysEntity;
import leonardo.banking_transactions.exceptions.accounts.AccountNotFoundException;
import leonardo.banking_transactions.exceptions.pixKeys.PixKeyDataConflictExistingRecord;
import leonardo.banking_transactions.exceptions.pixKeys.PixKeyNotFoundException;
import leonardo.banking_transactions.repositories.AccountsRepository;
import leonardo.banking_transactions.repositories.PixKeysRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class PixKeysService {
    private final PixKeysRepository pixKeysRepository;
    private final AccountsRepository accountsRepository;

    public PixKeysService(PixKeysRepository pixKeysRepository, AccountsRepository accountsRepository) {
        this.pixKeysRepository = pixKeysRepository;
        this.accountsRepository = accountsRepository;
    }

    @Transactional
    public PixKeysEntity create(UUID authenticatedUserId, UUID accountId, PixKeyCreateRequest request) {
        var account = accountsRepository
                .findByIdAndUserId(accountId, authenticatedUserId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (pixKeysRepository.existsByKeyValue(request.keyValue()))
            throw new PixKeyDataConflictExistingRecord();
        var key = new PixKeysEntity();
        key.setAccount(account);
        key.setKeyType(request.keyType());
        key.setKeyValue(request.keyValue());
        return pixKeysRepository.save(key);
    }

    @Transactional(readOnly = true)
    public List<PixKeysEntity> findByAccount(UUID authenticatedUserId, UUID accountId) {
        ensureAccountOwnedByUser(authenticatedUserId, accountId);
        return pixKeysRepository.findByAccountIdAndAccountUserId(accountId, authenticatedUserId);
    }

    @Transactional(readOnly = true)
    public PixKeysEntity findById(UUID authenticatedUserId, UUID accountId, UUID id) {
        return pixKeysRepository.findByIdAndAccountIdAndAccountUserId(id, accountId, authenticatedUserId)
                .orElseThrow(() -> new PixKeyNotFoundException(id));
    }

    @Transactional
    public PixKeysEntity update(UUID authenticatedUserId, UUID accountId, UUID id, PixKeyUpdateRequest request) {
        var key = findById(authenticatedUserId, accountId, id);
        if (!key.getKeyValue().equals(request.keyValue()) && pixKeysRepository.existsByKeyValue(request.keyValue()))
            throw new PixKeyDataConflictExistingRecord();
        key.setKeyType(request.keyType());
        key.setKeyValue(request.keyValue());
        key.setActive(request.active());
        return pixKeysRepository.save(key);
    }

    @Transactional
    public void delete(UUID authenticatedUserId, UUID accountId, UUID id) {
        pixKeysRepository.delete(findById(authenticatedUserId, accountId, id));
    }

    private void ensureAccountOwnedByUser(UUID userId, UUID accountId) {
        if (!accountsRepository.existsByIdAndUserId(accountId, userId))
            throw new AccountNotFoundException(accountId);
    }
}