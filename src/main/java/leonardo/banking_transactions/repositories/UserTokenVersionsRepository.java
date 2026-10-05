package leonardo.banking_transactions.repositories;

import leonardo.banking_transactions.entities.UserTokenVersionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserTokenVersionsRepository extends JpaRepository<UserTokenVersionsEntity, UUID> {
    boolean existsByUserIdAndTokenVersion(UUID userId, int tokenVersion);
}