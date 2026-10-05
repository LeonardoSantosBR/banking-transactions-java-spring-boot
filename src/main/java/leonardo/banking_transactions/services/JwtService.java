package leonardo.banking_transactions.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import leonardo.banking_transactions.entities.UsersEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration expiration;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration:3600}") long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiration = Duration.ofSeconds(expirationSeconds);
    }

    public String generateToken(UsersEntity user, int tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString()).claim("cpf", user.getCpf())
                .claim("tokenVersion", tokenVersion)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(expiration)))
                .signWith(key).compact();
    }

    public UUID extractUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        if (subject == null || subject.isBlank())
            throw new JwtException("JWT subject is missing");
        return UUID.fromString(subject);
    }

    public int extractTokenVersion(String token) {
        Integer version = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("tokenVersion", Integer.class);
        if (version == null || version < 0)
            throw new JwtException("JWT token version is missing or invalid");
        return version;
    }
}
