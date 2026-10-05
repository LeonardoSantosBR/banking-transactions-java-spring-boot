package leonardo.banking_transactions.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_token_versions")
@Getter
@Setter
@NoArgsConstructor
public class UserTokenVersionsEntity {
    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    public UserTokenVersionsEntity(UUID userId, int tokenVersion) {
        this.userId = userId;
        this.tokenVersion = tokenVersion;
    }
}