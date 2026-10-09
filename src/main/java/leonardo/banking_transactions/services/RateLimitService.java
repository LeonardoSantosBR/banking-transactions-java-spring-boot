package leonardo.banking_transactions.services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import leonardo.banking_transactions.config.RateLimitOperationConfig;
import leonardo.banking_transactions.exceptions.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class RateLimitService {
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int CPF_DIGITS = 11;
    private final ProxyManager<String> buckets;
    private final byte[] hmacKey;

    public RateLimitService(
            ProxyManager<String> buckets,
            @Value("${app.security.login-rate-limit-hmac-key}") String hmacKey) {
        this.buckets = buckets;
        this.hmacKey = hmacKey.getBytes(StandardCharsets.UTF_8);
    }


    public void checkAndConsume(RateLimitOperationConfig operation, String clientIp, String cpf) {
        String scope = operation.bucketScope();
        if (!consume(scope + ":ip:" + clientIp, operation.ipLimit()))
            throw new RateLimitExceededException();
        String normalizedCpf = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (normalizedCpf.length() != CPF_DIGITS)
            return;
        if (!consume(scope + ":cpf:" + hmac(normalizedCpf), operation.cpfLimit()))
            throw new RateLimitExceededException();
    }

    private boolean consume(String key, long capacity) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, WINDOW).build())
                .build();
        var bucket = buckets.getProxy(key, () -> configuration);
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

