package leonardo.banking_transactions.repositories;

import leonardo.banking_transactions.entities.AccountsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountsRepository extends JpaRepository<AccountsEntity, UUID> {
    boolean existsByBranchAndAccountNumber(String branch, String accountNumber);

    List<AccountsEntity> findByUserId(UUID userId);

    Optional<AccountsEntity> findByIdAndUserId(UUID id, UUID userId);
}
