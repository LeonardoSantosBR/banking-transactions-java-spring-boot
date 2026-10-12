package leonardo.banking_transactions.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import leonardo.banking_transactions.dtos.transactions.TransactionCreateRequest;
import leonardo.banking_transactions.entities.TransactionsEntity;
import leonardo.banking_transactions.enums.TransactionStatusEnum;
import leonardo.banking_transactions.exceptions.accounts.AccountNotFoundException;
import leonardo.banking_transactions.exceptions.transactions.TransactionDataConflictExistingRecord;
import leonardo.banking_transactions.exceptions.transactions.TransactionNotFoundException;
import leonardo.banking_transactions.exceptions.validations.InvalidTransactionException;
import leonardo.banking_transactions.repositories.AccountsRepository;
import leonardo.banking_transactions.repositories.TransactionsRepository;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionsService {
    private final TransactionsRepository transactionsRepository;
    private final AccountsRepository accountsRepository;
    private final OutboxEventsService outboxEventsService;
    private final ObjectMapper objectMapper;
    private final BalanceService balanceService;
    private final TransactionTemplate transactionTemplate;

    public TransactionsService(
            TransactionsRepository transactionsRepository,
            AccountsRepository accountsRepository,
            OutboxEventsService outboxEventsService,
            ObjectMapper objectMapper,
            BalanceService balanceService,
            TransactionTemplate transactionTemplate) {
        this.transactionsRepository = transactionsRepository;
        this.accountsRepository = accountsRepository;
        this.outboxEventsService = outboxEventsService;
        this.objectMapper = objectMapper;
        this.balanceService = balanceService;
        this.transactionTemplate = transactionTemplate;
    }

    public TransactionsEntity create(TransactionCreateRequest request) {
        var existing = transactionsRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent())
            return existing.get();

        try {
            return transactionTemplate.execute(status -> createInTransaction(request));
        } catch (DataIntegrityViolationException exception) {
            return transactionsRepository
                    .findByIdempotencyKey(request.idempotencyKey())
                    .orElseThrow(TransactionDataConflictExistingRecord::new);
        }
    }

    @Transactional(readOnly = true)
    public List<TransactionsEntity> findAll(UUID userId) {
        return transactionsRepository.findAllByPayerAccount_User_Id(userId);
    }

    @Transactional(readOnly = true)
    public TransactionsEntity findById(UUID id, UUID userId) {
        return transactionsRepository.findByIdAndPayerAccount_User_Id(id, userId)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional
    public void processSettlement(UUID id, boolean rejected) {
        var transaction = transactionsRepository
                .findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        if (transaction.getStatus() != TransactionStatusEnum.PROCESSING)
            return;

        if (rejected) {
            transaction.setStatus(TransactionStatusEnum.REJECTED);
        } else if (balanceService.transfer(
                transaction.getPayerAccount(),
                transaction.getPayeeAccount(),
                transaction.getAmount())) {
            transaction.setStatus(TransactionStatusEnum.SETTLED);
        } else {
            transaction.setStatus(TransactionStatusEnum.REJECTED);
            transaction.setDescription("Rejected: insufficient balance");
        }
        transactionsRepository.save(transaction);
    }

    private TransactionsEntity createInTransaction(TransactionCreateRequest request) {
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
        var transaction = transactionsRepository.save(t);
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("amount", transaction.getAmount());
        payload.put("currency", transaction.getCurrency());
        payload.put("pixKeyUsed", transaction.getPixKeyUsed());
        payload.put("payerAccountId", payer.getId().toString());
        payload.put("payeeAccountId", payee.getId().toString());
        payload.put("qrCodeType", transaction.getQrCodeType());
        payload.put("txid", transaction.getTxid());
        outboxEventsService.create(transaction, "PixTransactionRequested", payload);
        return transaction;
    }
}
