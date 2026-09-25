package leonardo.banking_transactions.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import leonardo.banking_transactions.enums.TransactionStatusEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_end_to_end_id", columnNames = "end_to_end_id"),
        @UniqueConstraint(name = "uk_transactions_idempotency_key", columnNames = "idempotency_key")
})
@Getter
@Setter
@NoArgsConstructor
public class TransactionsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "end_to_end_id", nullable = false, length = 50)
    private String endToEndId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_account_id", nullable = false)
    private AccountsEntity payerAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payee_account_id", nullable = false)
    private AccountsEntity payeeAccount;

    @Column(name = "pix_key_used", nullable = false, length = 150)
    private String pixKeyUsed;

    @Column(name = "qr_code_type", length = 20)
    private String qrCodeType;

    @Column(name = "qr_code_payload", columnDefinition = "TEXT")
    private String qrCodePayload;

    @Column(length = 100)
    private String txid;

    @Column(nullable = false, length = 3)
    private String currency = "BRL";

    @Column(length = 30)
    private String channel;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "transaction_status")
    private TransactionStatusEnum status = TransactionStatusEnum.PROCESSING;

    @Column(length = 200)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
