package leonardo.banking_transactions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import leonardo.banking_transactions.entity.SecurityAuditEventEntity;

import java.util.UUID;

public interface SecurityAuditEventsRepository extends JpaRepository<SecurityAuditEventEntity, UUID> {
}
