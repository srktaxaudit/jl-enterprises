package in.jlenterprises.ecommerce.security;

import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.ApiException;
import in.jlenterprises.ecommerce.service.impl.AuthServiceImpl;
import in.jlenterprises.ecommerce.util.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The brute-force protections hardened in audit #2. */
class AccountLockoutTest {

    // ── The failed-attempt counter must survive the login failure ──

    /**
     * login() records the failed attempt and THEN throws ApiException. Without
     * noRollbackFor, that throw rolls the counter write back — so the account lockout
     * could never engage, no matter how many wrong passwords were tried. This pins the
     * annotation so a future cleanup can't silently reintroduce the dead lockout.
     */
    @Test
    void loginTransactionMustNotRollBackTheFailedAttemptCounter() throws Exception {
        Method login = Arrays.stream(AuthServiceImpl.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("login"))
                .findFirst().orElseThrow();
        Transactional tx = login.getAnnotation(Transactional.class);
        assertNotNull(tx, "login() must stay @Transactional");
        assertTrue(Arrays.asList(tx.noRollbackFor()).contains(ApiException.class),
                "login() must declare noRollbackFor = ApiException.class — without it the "
                        + "failed-attempt write is rolled back and lockout never engages");
    }

    // ── Lock expiry: a failed-login lock is temporary, not forever ──

    @Test
    void lockExpiresAfterLockedUntilPasses() {
        User user = lockedUser(Instant.now().minusSeconds(60));   // lock window already over
        assertTrue(UserPrincipal.from(user).isAccountNonLocked(),
                "an expired lock must clear automatically — the user cannot log in to reset it");
    }

    @Test
    void lockHoldsWhileLockedUntilIsInTheFuture() {
        User user = lockedUser(Instant.now().plusSeconds(600));
        assertFalse(UserPrincipal.from(user).isAccountNonLocked());
    }

    @Test
    void lockWithoutExpiryIsIndefinite() {
        User user = lockedUser(null);   // admin-style manual lock
        assertFalse(UserPrincipal.from(user).isAccountNonLocked());
    }

    @Test
    void unlockedAccountStaysUnlocked() {
        User user = new User();
        user.setEmail("c@example.com");
        assertTrue(UserPrincipal.from(user).isAccountNonLocked());
    }

    private static User lockedUser(Instant lockedUntil) {
        User user = new User();
        user.setEmail("c@example.com");
        user.setAccountLocked(true);
        user.setLockedUntil(lockedUntil);
        return user;
    }

    // ── Client IP: the proxy-appended (last) X-Forwarded-For entry, never the first ──

    @Test
    void clientIpUsesLastForwardedEntry() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("6.6.6.6, 203.0.113.9");

        // 6.6.6.6 is attacker-chosen; 203.0.113.9 is what the proxy actually saw.
        assertEquals("203.0.113.9", ClientIp.from(req));
    }

    @Test
    void clientIpFallsBackToRemoteAddr() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("198.51.100.7");

        assertEquals("198.51.100.7", ClientIp.from(req));
    }

    @Test
    void clientIpHandlesSingleEntryAndWhitespace() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.9  ");

        assertEquals("203.0.113.9", ClientIp.from(req));
    }
}
