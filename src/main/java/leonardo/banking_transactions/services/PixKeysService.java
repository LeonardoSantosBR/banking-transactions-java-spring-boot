package leonardo.banking_transactions.services;

import leonardo.banking_transactions.dtos.PixKeyCreateRequest;
import leonardo.banking_transactions.dtos.PixKeyUpdateRequest;
import leonardo.banking_transactions.entities.PixKeysEntity;
import leonardo.banking_transactions.exceptions.AccountNotFoundException;
import leonardo.banking_transactions.exceptions.PixKeyAlreadyRegisteredException;
import leonardo.banking_transactions.exceptions.PixKeyNotFoundException;
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
    public PixKeysEntity create(UUID accountId, PixKeyCreateRequest request) {
        var account = accountsRepository
                .findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (pixKeysRepository.existsByKeyValue(request.keyValue()))
            throw new PixKeyAlreadyRegisteredException(request.keyValue());
        var key = new PixKeysEntity();
        key.setAccount(account);
        key.setKeyType(request.keyType());
        key.setKeyValue(request.keyValue());
        return pixKeysRepository.save(key);
    }

    @Transactional(readOnly = true)
    public List<PixKeysEntity> findByAccount(UUID accountId) {
        ensureAccount(accountId);
        return pixKeysRepository.findByAccountId(accountId);
    }

    @Transactional(readOnly = true)
    public PixKeysEntity findById(UUID accountId, UUID id) {
        return pixKeysRepository.findByIdAndAccountId(id, accountId).orElseThrow(() -> new PixKeyNotFoundException(id));
    }

    @Transactional
    public PixKeysEntity update(UUID accountId, UUID id, PixKeyUpdateRequest request) {
        var key = findById(accountId, id);
        if (!key.getKeyValue().equals(request.keyValue()) && pixKeysRepository.existsByKeyValue(request.keyValue()))
            throw new PixKeyAlreadyRegisteredException(request.keyValue());
        key.setKeyType(request.keyType());
        key.setKeyValue(request.keyValue());
        key.setActive(request.active());
        return pixKeysRepository.save(key);
    }

    @Transactional
    public void delete(UUID accountId, UUID id) {
        pixKeysRepository.delete(findById(accountId, id));
    }

    private void ensureAccount(UUID id) {
        if (!accountsRepository.existsById(id))
            throw new AccountNotFoundException(id);
    }
}
