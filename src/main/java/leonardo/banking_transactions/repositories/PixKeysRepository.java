package leonardo.banking_transactions.repositories;

import leonardo.banking_transactions.entities.PixKeysEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PixKeysRepository extends JpaRepository<PixKeysEntity, UUID> {
    boolean existsByKeyValue(String keyValue);

    List<PixKeysEntity> findByAccountId(UUID accountId);

    Optional<PixKeysEntity> findByIdAndAccountId(UUID id, UUID accountId);
}
