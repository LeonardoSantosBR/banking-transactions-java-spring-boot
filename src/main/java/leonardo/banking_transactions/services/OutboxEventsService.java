package leonardo.banking_transactions.services;

import com.fasterxml.jackson.databind.JsonNode;
import leonardo.banking_transactions.entities.OutboxEventsEntity;
import leonardo.banking_transactions.entities.TransactionsEntity;
import leonardo.banking_transactions.enums.OutboxEventStatusEnum;
import leonardo.banking_transactions.repositories.OutboxEventsRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OutboxEventsService {
    private final OutboxEventsRepository repository;
    private final SqsService sqsService;

    public OutboxEventsService(OutboxEventsRepository repository, SqsService sqsService) {
        this.repository = repository;
        this.sqsService = sqsService;
    }

    @Transactional(readOnly = true)
    public List<OutboxEventsEntity> findPending(int batchSize) {
        if (batchSize < 1)
            throw new IllegalArgumentException("Batch size must be greater than zero");
        return repository.findByStatusOrderByCreatedAtAsc(
                OutboxEventStatusEnum.PENDING, PageRequest.of(0, batchSize));
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
        markAsPublished(event);
    }
}
