package leonardo.banking_transactions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import leonardo.banking_transactions.entity.PixKeysEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PixKeysRepository extends JpaRepository<PixKeysEntity, UUID> {
    boolean existsByKeyValue(String keyValue);
    Optional<PixKeysEntity> findByKeyValueAndActiveTrue(String keyValue);
    List<PixKeysEntity> findByAccountIdAndAccountUserId(UUID accountId, UUID userId);
    Optional<PixKeysEntity> findByIdAndAccountIdAndAccountUserId(UUID id, UUID accountId, UUID userId);
}
