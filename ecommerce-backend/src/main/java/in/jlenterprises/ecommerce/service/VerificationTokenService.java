package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.exception.InvalidTokenException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Opaque, single-use tokens for email-verification and password-reset links,
 * stored in Redis with a TTL. The token maps to the target email; consuming it
 * deletes it so a link works only once.
 */
@Service
public class VerificationTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;

    public VerificationTokenService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String issue(String purpose, String email, Duration ttl) {
        String token = generate();
        redis.opsForValue().set(key(purpose, token), email.toLowerCase(), ttl);
        return token;
    }

    /** @return the email the token was issued for; the token is then invalidated. */
    public String consume(String purpose, String token) {
        String k = key(purpose, token);
        String email = redis.opsForValue().get(k);
        if (email == null) {
            throw new InvalidTokenException("Invalid or expired token");
        }
        redis.delete(k);
        return email;
    }

    private String key(String purpose, String token) {
        return "vtoken:" + purpose + ":" + token;
    }

    private String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
