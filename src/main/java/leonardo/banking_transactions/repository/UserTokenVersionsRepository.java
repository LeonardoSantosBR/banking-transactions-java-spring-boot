package leonardo.banking_transactions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import leonardo.banking_transactions.entity.UserTokenVersionsEntity;

import java.util.UUID;

public interface UserTokenVersionsRepository extends JpaRepository<UserTokenVersionsEntity, UUID> {
    boolean existsByUserIdAndTokenVersion(UUID userId, int tokenVersion);
}