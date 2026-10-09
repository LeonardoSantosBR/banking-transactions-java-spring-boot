package leonardo.banking_transactions.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import leonardo.banking_transactions.enums.SecurityAuditEventTypeEnum;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "security_audit_events")
public class SecurityAuditEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private SecurityAuditEventTypeEnum eventType;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "subject_user_id")
    private UUID subjectUserId;

    @Column(name = "subject_identifier_hmac", length = 64)
    private String subjectIdentifierHmac;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt = OffsetDateTime.now();

    protected SecurityAuditEventEntity() {
    }

    public SecurityAuditEventEntity(
            SecurityAuditEventTypeEnum eventType,
            String subjectIdentifierHmac,
            String sourceIp,
            String userAgent) {
        this.eventType = eventType;
        this.subjectIdentifierHmac = subjectIdentifierHmac;
        this.sourceIp = sourceIp;
        this.userAgent = userAgent;
    }
}
