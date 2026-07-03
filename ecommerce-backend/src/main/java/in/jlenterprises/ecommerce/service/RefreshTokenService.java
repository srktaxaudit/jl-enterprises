package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.config.AppProperties;
import in.jlenterprises.ecommerce.entity.RefreshToken;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.InvalidTokenException;
import in.jlenterprises.ecommerce.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Issues, validates and rotates opaque refresh tokens (stored server-side so
 * they can be revoked). Access tokens stay short-lived; refresh tokens are
 * rotated on every use to limit replay.
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
        RefreshToken rt = new RefreshToken();
        rt.setToken(generate());
        rt.setUser(user);
        rt.setRememberMe(rememberMe);
        rt.setExpiresAt(Instant.now().plus(rememberMe ? rememberMeTtl : defaultTtl));
        rt.setUserAgent(userAgent);
        rt.setIpAddress(ip);
        return repository.save(rt);
    }

    @Transactional(readOnly = true)
    public RefreshToken verify(String token) {
        RefreshToken rt = repository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
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
        repository.findByToken(token).ifPresent(rt -> {
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
}
