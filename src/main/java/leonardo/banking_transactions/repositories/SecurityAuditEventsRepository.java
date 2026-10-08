package leonardo.banking_transactions.repositories;

import leonardo.banking_transactions.entities.SecurityAuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SecurityAuditEventsRepository extends JpaRepository<SecurityAuditEventEntity, UUID> {
}
