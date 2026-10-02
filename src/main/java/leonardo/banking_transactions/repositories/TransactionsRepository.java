package leonardo.banking_transactions.repositories;

import leonardo.banking_transactions.entities.TransactionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface TransactionsRepository extends JpaRepository<TransactionsEntity, UUID> {
    Optional<TransactionsEntity> findByIdempotencyKey(String idempotencyKey);
    boolean existsByIdempotencyKey(String idempotencyKey);
    List<TransactionsEntity> findAllByPayerAccount_User_Id(UUID userId);
    Optional<TransactionsEntity> findByIdAndPayerAccount_User_Id(UUID id, UUID userId);
}
