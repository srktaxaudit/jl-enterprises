package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.config.AppProperties;
import in.jlenterprises.ecommerce.entity.RefreshToken;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.InvalidTokenException;
import in.jlenterprises.ecommerce.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Issues, validates and rotates opaque refresh tokens (stored server-side so
 * they can be revoked). Access tokens stay short-lived; refresh tokens are
 * rotated on every use to limit replay.
 *
 * <p>Tokens are stored <b>hashed</b> (SHA-256) at rest — a leaked DB row can't be
 * replayed. Rotation revokes the presented token; if an already-revoked token is
 * presented again (theft/replay), the whole token family for that user is revoked.
 * Legacy plaintext rows still validate (fallback lookup) so no one is logged out on upgrade.
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final Duration defaultTtl;
    private final Duration rememberMeTtl;

    public RefreshTokenService(RefreshTokenRepository repository, AppProperties props) {
        this.repository = repository;
        this.defaultTtl = props.jwt().refreshTokenTtl();
        this.rememberMeTtl = props.jwt().rememberMeRefreshTtl();
    }

    @Transactional
    public RefreshToken issue(User user, boolean rememberMe, String userAgent, String ip) {
        String raw = generate();
        RefreshToken rt = new RefreshToken();
        rt.setToken(sha256(raw));                 // store only the hash
        rt.setUser(user);
        rt.setRememberMe(rememberMe);
        rt.setExpiresAt(Instant.now().plus(rememberMe ? rememberMeTtl : defaultTtl));
        rt.setUserAgent(userAgent);
        rt.setIpAddress(ip);
        repository.save(rt);
        rt.setRawToken(raw);                      // hand the raw value back to the caller (never persisted)
        return rt;
    }

    @Transactional
    public RefreshToken verify(String token) {
        RefreshToken rt = repository.findByToken(sha256(token))
                .or(() -> repository.findByToken(token))   // legacy plaintext rows (pre-hashing)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        if (rt.isRevoked()) {
            // An already-rotated token presented again → likely theft/replay. Revoke the
            // whole family for this user as a defensive response.
            repository.revokeAllForUser(rt.getUser());
            throw new InvalidTokenException("Refresh token reuse detected; all sessions revoked");
        }
        if (!rt.isActive()) {
            throw new InvalidTokenException("Refresh token expired or revoked");
        }
        return rt;
    }

    /** Rotate: revoke the presented token and issue a fresh one for the same user/device. */
    @Transactional
    public RefreshToken rotate(RefreshToken current, String userAgent, String ip) {
        current.setRevoked(true);
        repository.save(current);
        return issue(current.getUser(), current.isRememberMe(), userAgent, ip);
    }

    @Transactional
    public void revoke(String token) {
        repository.findByToken(sha256(token))
                .or(() -> repository.findByToken(token))   // legacy plaintext rows
                .ifPresent(rt -> {
                    rt.setRevoked(true);
                    repository.save(rt);
                });
    }

    @Transactional
    public void revokeAll(User user) {
        repository.revokeAllForUser(user);
    }

    private String generate() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);   // never on a standard JRE
        }
    }
}
