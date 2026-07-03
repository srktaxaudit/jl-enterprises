package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.config.AppProperties;
import in.jlenterprises.ecommerce.constant.OtpPurpose;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.InvalidTokenException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

/**
 * Numeric OTP generation and verification, backed by Redis with a TTL and an
 * attempt cap. Only the BCrypt hash of the code is stored — never the code.
 */
@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redis;
    private final PasswordEncoder encoder;
    private final int length;
    private final Duration ttl;
    private final int maxAttempts;

    public OtpService(StringRedisTemplate redis, PasswordEncoder encoder, AppProperties props) {
        this.redis = redis;
        this.encoder = encoder;
        this.length = props.otp().length();
        this.ttl = props.otp().ttl();
        this.maxAttempts = props.otp().maxAttempts();
    }

    /** Generate a code, store its hash, and return the plain code for delivery (SMS/email). */
    public String generate(String identifier, OtpPurpose purpose) {
        String code = randomDigits(length);
        redis.opsForValue().set(codeKey(identifier, purpose), encoder.encode(code), ttl);
        redis.delete(attemptsKey(identifier, purpose));
        return code;
    }

    /** Verify a code; consumes it on success. Throws on expiry, mismatch or too many attempts. */
    public void verify(String identifier, OtpPurpose purpose, String code) {
        String stored = redis.opsForValue().get(codeKey(identifier, purpose));
        if (stored == null) {
            throw new InvalidTokenException("OTP expired or not requested");
        }
        Long attempts = redis.opsForValue().increment(attemptsKey(identifier, purpose));
        redis.expire(attemptsKey(identifier, purpose), ttl);
        if (attempts != null && attempts > maxAttempts) {
            redis.delete(codeKey(identifier, purpose));
            throw new BusinessException("Too many incorrect attempts. Request a new OTP.");
        }
        if (!encoder.matches(code, stored)) {
            throw new InvalidTokenException("Incorrect OTP");
        }
        redis.delete(codeKey(identifier, purpose));
        redis.delete(attemptsKey(identifier, purpose));
    }

    private String codeKey(String identifier, OtpPurpose purpose) {
        return "otp:" + purpose + ":" + identifier.toLowerCase();
    }

    private String attemptsKey(String identifier, OtpPurpose purpose) {
        return "otp:attempts:" + purpose + ":" + identifier.toLowerCase();
    }

    private static String randomDigits(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(RANDOM.nextInt(10));
        return sb.toString();
    }
}
