package leonardo.banking_transactions.services;

import leonardo.banking_transactions.entities.SecurityAuditEventEntity;
import leonardo.banking_transactions.entities.SecurityAuditEventType;
import leonardo.banking_transactions.repositories.SecurityAuditEventsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Service
public class SecurityAuditService {
    private final SecurityAuditEventsRepository repository;
    private final byte[] hmacKey;

    public SecurityAuditService(
            SecurityAuditEventsRepository repository,
            @Value("${app.security.login-rate-limit-hmac-key}") String hmacKey) {
        this.repository = repository;
        this.hmacKey = hmacKey.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            SecurityAuditEventType eventType,
            String cpf,
            String sourceIp,
            String userAgent) {
        repository.save(new SecurityAuditEventEntity(
                eventType,
                cpf == null ? null : hmac(normalizeCpf(cpf)),
                sourceIp,
                truncate(userAgent, 512)));
    }

    private String normalizeCpf(String cpf) {
        return cpf.replaceAll("\\D", "");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength)
            return value;
        return value.substring(0, maxLength);
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to hash audit subject identifier", exception);
        }
    }
}
