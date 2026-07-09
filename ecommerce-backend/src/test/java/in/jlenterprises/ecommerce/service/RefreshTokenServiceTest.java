package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.config.AppProperties;
import in.jlenterprises.ecommerce.entity.RefreshToken;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.InvalidTokenException;
import in.jlenterprises.ecommerce.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository repository;

    private RefreshTokenService service() {
        AppProperties props = new AppProperties(
                new AppProperties.Jwt("secret", Duration.ofMinutes(15), Duration.ofDays(7), Duration.ofDays(30), "iss"),
                null, null, null, null, null);
        return new RefreshTokenService(repository, props);
    }

    @Test
    void issue_storesHash_returnsRawToClient() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken rt = service().issue(new User(), false, "ua", "1.2.3.4");

        assertNotNull(rt.getRawToken(), "raw token must be handed back to the caller");
        assertNotEquals(rt.getRawToken(), rt.getToken(), "the raw token must not be what is stored");
        assertEquals(64, rt.getToken().length(), "stored value is a SHA-256 hex digest");
    }

    @Test
    void verify_detectsReuseOfRevokedToken_andRevokesFamily() {
        User user = new User();
        RefreshToken revoked = new RefreshToken();
        revoked.setUser(user);
        revoked.setRevoked(true);
        revoked.setExpiresAt(Instant.now().plusSeconds(3600));
        when(repository.findByToken(anyString())).thenReturn(Optional.of(revoked));

        assertThrows(InvalidTokenException.class, () -> service().verify("stolen-token"));
        verify(repository).revokeAllForUser(user);   // whole family revoked on reuse
    }
}
