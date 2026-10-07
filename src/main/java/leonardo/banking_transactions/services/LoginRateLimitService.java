package leonardo.banking_transactions.services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import leonardo.banking_transactions.exceptions.LoginRateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class LoginRateLimitService {
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final ProxyManager<String> buckets;
    private final byte[] hmacKey;

    public LoginRateLimitService(
            ProxyManager<String> buckets,
            @Value("${app.security.login-rate-limit-hmac-key}") String hmacKey) {
        this.buckets = buckets;
        this.hmacKey = hmacKey.getBytes(StandardCharsets.UTF_8);
    }

    public void checkAndConsumeLogin(String clientIp, String cpf) {
        checkAndConsume("login", clientIp, cpf, 30, 5);
    }

    public void checkAndConsumeUserCreation(String clientIp, String cpf) {
        checkAndConsume("registration", clientIp, cpf, 10, 3);
    }

    private void checkAndConsume(String scope, String clientIp, String cpf, long ipLimit, long cpfLimit) {
        if (!consume(scope + ":ip:" + clientIp, ipLimit))
            throw new LoginRateLimitExceededException();
        String normalizedCpf = cpf.replaceAll("\\D", "");
        if (!consume(scope + ":cpf:" + hmac(normalizedCpf), cpfLimit))
            throw new LoginRateLimitExceededException();
    }

    private boolean consume(String key, long capacity) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, WINDOW).build())
                .build();
        var bucket = buckets.builder().build(key, configuration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        return probe.isConsumed();
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to create rate limit key", exception);
        }
    }
}
