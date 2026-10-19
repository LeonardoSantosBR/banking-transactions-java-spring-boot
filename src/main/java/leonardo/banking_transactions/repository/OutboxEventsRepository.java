package leonardo.banking_transactions.repository;

import leonardo.banking_transactions.entity.OutboxEventsEntity;
import leonardo.banking_transactions.enums.OutboxEventStatusEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface OutboxEventsRepository extends JpaRepository<OutboxEventsEntity, UUID> {
    @EntityGraph(attributePaths = { "transaction", "transaction.payerAccount" })
    List<OutboxEventsEntity> findByStatusOrderByCreatedAtAsc(OutboxEventStatusEnum status, Pageable pageable);

    Optional<OutboxEventsEntity> findByIdAndTransactionIdAndEventType(UUID id, UUID transactionId, String eventType);
}
