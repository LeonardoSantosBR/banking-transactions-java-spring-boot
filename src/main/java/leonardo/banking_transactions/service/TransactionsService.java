package leonardo.banking_transactions.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import leonardo.banking_transactions.dto.transactions.TransactionCreateRequest;
import leonardo.banking_transactions.entity.TransactionsEntity;
import leonardo.banking_transactions.enums.TransactionStatusEnum;
import leonardo.banking_transactions.exception.accounts.AccountNotFoundException;
import leonardo.banking_transactions.exception.transactions.TransactionDataConflictExistingRecord;
import leonardo.banking_transactions.exception.transactions.TransactionNotFoundException;
import leonardo.banking_transactions.exception.validations.InvalidTransactionException;
import leonardo.banking_transactions.repository.AccountsRepository;
import leonardo.banking_transactions.repository.PixKeysRepository;
import leonardo.banking_transactions.repository.TransactionsRepository;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@Service
public class TransactionsService {
    private final TransactionsRepository transactionsRepository;
    private final AccountsRepository accountsRepository;
    private final OutboxEventsService outboxEventsService;
    private final ObjectMapper objectMapper;
    private final BalanceService balanceService;
    private final TransactionTemplate transactionTemplate;
    private final PixKeysRepository pixKeysRepository;
    private final PixPayloadValidator pixPayloadValidator;

    public TransactionsService(
            TransactionsRepository transactionsRepository,
            AccountsRepository accountsRepository,
            OutboxEventsService outboxEventsService,
            ObjectMapper objectMapper,
            BalanceService balanceService,
            TransactionTemplate transactionTemplate,
            PixKeysRepository pixKeysRepository,
            PixPayloadValidator pixPayloadValidator) {
        this.transactionsRepository = transactionsRepository;
        this.accountsRepository = accountsRepository;
        this.outboxEventsService = outboxEventsService;
        this.objectMapper = objectMapper;
        this.balanceService = balanceService;
        this.transactionTemplate = transactionTemplate;
        this.pixKeysRepository = pixKeysRepository;
        this.pixPayloadValidator = pixPayloadValidator;
    }

    public TransactionsEntity create(UUID payerId,TransactionCreateRequest requestBody) {
        var existing = transactionsRepository.findByIdempotencyKey(requestBody.idempotencyKey());
        if (existing.isPresent())
            return existing.get();
        try {
            return transactionTemplate.execute(status -> createInTransaction(payerId, requestBody));
        } catch (DataIntegrityViolationException exception) {
            return transactionsRepository
                    .findByIdempotencyKey(requestBody.idempotencyKey())
                    .orElseThrow(TransactionDataConflictExistingRecord::new);
        }
    }

    private TransactionsEntity createInTransaction(UUID payerId, TransactionCreateRequest body) {
        if (body.payerAccountId().equals(body.payeeAccountId()))
            throw new InvalidTransactionException("Payer and payee accounts must be different");
        var payer = accountsRepository.findByIdAndUserId(body.payerAccountId(), payerId)
                .orElseThrow(() -> new AccountNotFoundException(body.payerAccountId()));
        var payee = accountsRepository.findById(body.payeeAccountId())
                .orElseThrow(() -> new AccountNotFoundException(body.payeeAccountId()));
        var pixKey = pixKeysRepository.findByKeyValueAndActiveTrue(body.pixKeyUsed())
                .orElseThrow(() -> new InvalidTransactionException("Pix key is invalid or inactive"));
        if (!pixKey.getAccount().getId().equals(payee.getId()))
            throw new InvalidTransactionException("Pix key does not belong to payee account");
        pixPayloadValidator.validate(body.qrCodeType(), body.qrCodePayload(), body.txid());
        var t = new TransactionsEntity();
        t.setEndToEndId("E" + UUID.randomUUID().toString().replace("-", ""));
        t.setIdempotencyKey(body.idempotencyKey());
        t.setPayerAccount(payer);
        t.setPayeeAccount(payee);
        t.setPixKeyUsed(body.pixKeyUsed());
        t.setAmount(body.amount());
        t.setQrCodeType(body.qrCodeType() == null ? null : body.qrCodeType().name());
        t.setQrCodePayload(body.qrCodePayload());
        t.setTxid(body.txid());
        t.setCurrency(body.currency() == null ? "BRL" : body.currency().name());
        t.setChannel(body.channel() == null ? null : body.channel().name());
        t.setDescription(body.description());
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

    public TransactionsEntity refund(UUID transactionId, UUID userId) {
        return transactionTemplate.execute(status -> {
            var original = transactionsRepository.findByIdForUpdate(transactionId)
                    .orElseThrow(() -> new TransactionNotFoundException(transactionId));
            boolean owner = original.getPayerAccount().getUser().getId().equals(userId)
                    || original.getPayeeAccount().getUser().getId().equals(userId);
            if (!owner)
                throw new TransactionNotFoundException(transactionId);
            if (original.getStatus() != TransactionStatusEnum.SETTLED)
                throw new InvalidTransactionException("Only settled transactions can be refunded");
            var existing = transactionsRepository.findByRefundOfTransaction_Id(transactionId);
            if (existing.isPresent())
                return existing.get();
            var refund = new TransactionsEntity();
            refund.setRefundOfTransaction(original);
            refund.setEndToEndId("E" + UUID.randomUUID().toString().replace("-", ""));
            refund.setIdempotencyKey("refund:" + transactionId);
            refund.setPayerAccount(original.getPayeeAccount());
            refund.setPayeeAccount(original.getPayerAccount());
            refund.setPixKeyUsed(original.getPixKeyUsed());
            refund.setAmount(original.getAmount());
            refund.setCurrency(original.getCurrency());
            refund.setChannel(original.getChannel());
            refund.setTxid(original.getTxid());
            refund.setDescription("Refund of transaction " + transactionId);
            var saved = transactionsRepository.save(refund);
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("amount", saved.getAmount());
            payload.put("currency", saved.getCurrency());
            payload.put("pixKeyUsed", saved.getPixKeyUsed());
            payload.put("payerAccountId", saved.getPayerAccount().getId().toString());
            payload.put("payeeAccountId", saved.getPayeeAccount().getId().toString());
            payload.put("refundOfTransactionId", transactionId.toString());
            outboxEventsService.create(saved, "PixTransactionRefundRequested", payload);
            return saved;
        });
    }

    @Transactional(readOnly = true)
    public Page<TransactionsEntity> findAll(
            UUID userId,
            TransactionStatusEnum status,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            BigDecimal amountFrom,
            BigDecimal amountTo,
            String txid,
            String endToEndId,
            Pageable pageable) {
        Specification<TransactionsEntity> specification = (root, query, cb) -> cb.or(
                cb.equal(root.get("payerAccount").get("user").get("id"), userId),
                cb.equal(root.get("payeeAccount").get("user").get("id"), userId));

        if (status != null)
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (createdFrom != null)
            specification = specification
                    .and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        if (createdTo != null)
            specification = specification
                    .and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        if (amountFrom != null)
            specification = specification
                    .and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("amount"), amountFrom));
        if (amountTo != null)
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("amount"), amountTo));
        if (txid != null && !txid.isBlank())
            specification = specification.and((root, query, cb) -> cb.equal(root.get("txid"), txid));
        if (endToEndId != null && !endToEndId.isBlank())
            specification = specification.and((root, query, cb) -> cb.equal(root.get("endToEndId"), endToEndId));

        return transactionsRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public TransactionsEntity findById(UUID id, UUID userId) {
        return transactionsRepository.findByIdAndPayerAccount_User_Id(id, userId)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional
    public void validateConsistencyMessageSQS(UUID eventId, UUID id, String idempotencyKey, JsonNode payload,
            String eventType) {
        outboxEventsService.findByIdAndTransactionIdAndEventType(eventId, id, eventType)
                .orElseThrow(() -> new InvalidTransactionException("Transaction event is not registered"));
        var transaction = transactionsRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        if (!transaction.getIdempotencyKey().equals(idempotencyKey)
                || transaction.getAmount().compareTo(payload.path("amount").decimalValue()) != 0
                || !transaction.getCurrency().equals(payload.path("currency").asText())
                || !transaction.getPayerAccount().getId()
                        .equals(UUID.fromString(payload.path("payerAccountId").asText()))
                || !transaction.getPayeeAccount().getId()
                        .equals(UUID.fromString(payload.path("payeeAccountId").asText())))
            throw new InvalidTransactionException("Transaction event payload does not match stored transaction");
        if (eventType.equals("PixTransactionRequested")) {
            executeTransactionSettlement(id, payload.path("reject").asBoolean(false));
        } else {
            executeTransactionRefundSettlement(id);
        }
    }

    @Transactional
    public void executeTransactionRefundSettlement(UUID refundId) {
        var refund = transactionsRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new TransactionNotFoundException(refundId));
        if (refund.getStatus() != TransactionStatusEnum.PROCESSING)
            return;
        var original = refund.getRefundOfTransaction();
        if (original == null || original.getStatus() != TransactionStatusEnum.SETTLED)
            throw new InvalidTransactionException("Refund source transaction is not settled");
        if (!balanceService.transfer(refund.getPayerAccount(), refund.getPayeeAccount(), refund.getAmount()))
            throw new InvalidTransactionException("Refund failed due to insufficient balance");
        refund.setStatus(TransactionStatusEnum.REFUNDED);
        transactionsRepository.save(refund);
    }

    @Transactional
    public void executeTransactionSettlement(UUID id, boolean rejected) {
        var transaction = transactionsRepository
                .findByIdForUpdate(id)
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
}
