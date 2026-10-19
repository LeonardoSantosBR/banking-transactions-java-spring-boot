package leonardo.banking_transactions.service;

import com.fasterxml.jackson.databind.JsonNode;

import leonardo.banking_transactions.entity.OutboxEventsEntity;
import leonardo.banking_transactions.entity.TransactionsEntity;
import leonardo.banking_transactions.enums.OutboxEventStatusEnum;
import leonardo.banking_transactions.repository.OutboxEventsRepository;
import leonardo.banking_transactions.service.aws.SnsService;
import leonardo.banking_transactions.service.aws.SqsService;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OutboxEventsService {
    private final OutboxEventsRepository repository;
    private final SqsService sqsService;
    private final SnsService snsService;

    public OutboxEventsService(OutboxEventsRepository repository, SqsService sqsService, SnsService snsService) {
        this.repository = repository;
        this.sqsService = sqsService;
        this.snsService = snsService;
    }

    @Transactional(readOnly = true)
    public Optional<OutboxEventsEntity> findByIdAndTransactionIdAndEventType(UUID id, UUID transactionId,
            String eventType) {
        return repository.findByIdAndTransactionIdAndEventType(id, transactionId, eventType);
    }

    public List<OutboxEventsEntity> findPending(int batchSize) {
        if (batchSize < 1)
            throw new IllegalArgumentException("Batch size must be greater than zero");
        return repository.findByStatusOrderByCreatedAtAsc(OutboxEventStatusEnum.PENDING, PageRequest.of(0, batchSize));
    }

    @Transactional
    public OutboxEventsEntity create(TransactionsEntity transaction, String eventType, JsonNode payload) {
        var event = new OutboxEventsEntity();
        event.setTransaction(transaction);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus(OutboxEventStatusEnum.PENDING);
        return repository.save(event);
    }

    @Transactional
    public void markAsPublished(OutboxEventsEntity event) {
        event.setStatus(OutboxEventStatusEnum.PUBLISHED);
        event.setPublishedAt(OffsetDateTime.now());
        repository.save(event);
    }

    @Transactional
    public void markAsFailed(OutboxEventsEntity event) {
        event.setStatus(OutboxEventStatusEnum.FAILED);
        repository.save(event);
    }

    public void publish(OutboxEventsEntity event) {
        sqsService.sendTransactionEvent(event);
        snsService.publishTransactionEvent(event);
        markAsPublished(event);
    }
}
