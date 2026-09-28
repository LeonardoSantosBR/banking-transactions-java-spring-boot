package leonardo.banking_transactions.services;

import leonardo.banking_transactions.dtos.TransactionCreateRequest;
import leonardo.banking_transactions.dtos.TransactionUpdateRequest;
import leonardo.banking_transactions.entities.TransactionsEntity;
import leonardo.banking_transactions.exceptions.*;
import leonardo.banking_transactions.repositories.AccountsRepository;
import leonardo.banking_transactions.repositories.TransactionsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionsService {
    private final TransactionsRepository repository;
    private final AccountsRepository accountsRepository;

    public TransactionsService(TransactionsRepository repository, AccountsRepository accountsRepository) {
        this.repository = repository;
        this.accountsRepository = accountsRepository;
    }

    @Transactional
    public TransactionsEntity create(TransactionCreateRequest request) {
        if (repository.existsByIdempotencyKey(request.idempotencyKey()))
            throw new TransactionAlreadyRegisteredException(request.idempotencyKey());
        if (request.payerAccountId().equals(request.payeeAccountId()))
            throw new InvalidTransactionException("Payer and payee accounts must be different");
        var payer = accountsRepository.findById(request.payerAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.payerAccountId()));
        var payee = accountsRepository.findById(request.payeeAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.payeeAccountId()));
        var t = new TransactionsEntity();
        t.setEndToEndId("E" + UUID.randomUUID().toString().replace("-", ""));
        t.setIdempotencyKey(request.idempotencyKey());
        t.setPayerAccount(payer);
        t.setPayeeAccount(payee);
        t.setPixKeyUsed(request.pixKeyUsed());
        t.setAmount(request.amount());
        t.setQrCodeType(request.qrCodeType());
        t.setQrCodePayload(request.qrCodePayload());
        t.setTxid(request.txid());
        t.setCurrency(request.currency() == null ? "BRL" : request.currency());
        t.setChannel(request.channel());
        t.setDescription(request.description());
        return repository.save(t);
    }

    @Transactional(readOnly = true)
    public List<TransactionsEntity> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public TransactionsEntity findById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional
    public TransactionsEntity update(UUID id, TransactionUpdateRequest request) {
        var t = findById(id);
        t.setStatus(request.status());
        t.setDescription(request.description());
        return repository.save(t);
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(findById(id));
    }
}
