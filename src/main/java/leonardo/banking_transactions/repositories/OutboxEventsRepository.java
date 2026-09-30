package leonardo.banking_transactions.repositories;

import leonardo.banking_transactions.entities.OutboxEventsEntity;
import leonardo.banking_transactions.enums.OutboxEventStatusEnum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventsRepository extends JpaRepository<OutboxEventsEntity, UUID> {
    @EntityGraph(attributePaths = { "transaction", "transaction.payerAccount" })
    List<OutboxEventsEntity> findByStatusOrderByCreatedAtAsc(OutboxEventStatusEnum status, Pageable pageable);
}
