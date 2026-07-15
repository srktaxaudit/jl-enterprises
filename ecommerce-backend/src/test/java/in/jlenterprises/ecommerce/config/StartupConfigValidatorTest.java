package in.jlenterprises.ecommerce.config;

import org.junit.jupiter.api.Test;

import static in.jlenterprises.ecommerce.config.StartupConfigValidator.DEFAULT_DB_PASSWORD;
import static in.jlenterprises.ecommerce.config.StartupConfigValidator.DEFAULT_JWT_SECRET;
import static in.jlenterprises.ecommerce.config.StartupConfigValidator.validateProduction;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Production configuration must fail fast on unsafe values — and never leak the values themselves. */
class StartupConfigValidatorTest {

    private static final String GOOD_JWT = "a-real-production-secret-that-is-long-enough-32+";
    private static final String GOOD_DB_PASSWORD = "s3cure-db-password";
    private static final String GOOD_ADMIN_PASSWORD = "Str0ng!AdminPass2026";
    private static final String GOOD_CORS = "https://jlstores.in,https://www.jlstores.in";

    private static IllegalStateException expectFail(String jwt, String db, String admin, String cors) {
        return assertThrows(IllegalStateException.class, () -> validateProduction(jwt, db, admin, cors));
    }

    @Test
    void acceptsAValidProductionConfiguration() {
        assertDoesNotThrow(() -> validateProduction(GOOD_JWT, GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, GOOD_CORS));
    }

    // ── JWT secret ──
    @Test
    void rejectsMissingJwtSecret() {
        assertTrue(expectFail("", GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, GOOD_CORS).getMessage().contains("JWT_SECRET"));
        expectFail(null, GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, GOOD_CORS);
    }

    @Test
    void rejectsDefaultJwtSecret() {
        assertTrue(expectFail(DEFAULT_JWT_SECRET, GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, GOOD_CORS)
                .getMessage().contains("JWT_SECRET"));
    }

    @Test
    void rejectsShortJwtSecret() {
        assertTrue(expectFail("too-short-secret", GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, GOOD_CORS)
                .getMessage().contains("too short"));
    }

    // ── Database password ──
    @Test
    void rejectsMissingOrDefaultDbPassword() {
        assertTrue(expectFail(GOOD_JWT, "", GOOD_ADMIN_PASSWORD, GOOD_CORS).getMessage().contains("DB_PASSWORD"));
        assertTrue(expectFail(GOOD_JWT, DEFAULT_DB_PASSWORD, GOOD_ADMIN_PASSWORD, GOOD_CORS)
                .getMessage().contains("DB_PASSWORD"));
    }

    // ── Bootstrap admin password ──
    @Test
    void rejectsMissingWeakOrDefaultAdminPassword() {
        assertTrue(expectFail(GOOD_JWT, GOOD_DB_PASSWORD, "", GOOD_CORS)
                .getMessage().contains("APP_BOOTSTRAP_ADMIN_PASSWORD"));
        // The DataInitializer sentinel default.
        assertTrue(expectFail(GOOD_JWT, GOOD_DB_PASSWORD, DataInitializer.DEFAULT_ADMIN_PASSWORD, GOOD_CORS)
                .getMessage().contains("APP_BOOTSTRAP_ADMIN_PASSWORD"));
        // A well-known weak value (case-insensitive).
        expectFail(GOOD_JWT, GOOD_DB_PASSWORD, "ChangeMe", GOOD_CORS);
        // Too short.
        assertTrue(expectFail(GOOD_JWT, GOOD_DB_PASSWORD, "Sh0rt!", GOOD_CORS).getMessage().contains("too short"));
    }

    // ── CORS ──
    @Test
    void rejectsMissingCorsOrigins() {
        assertTrue(expectFail(GOOD_JWT, GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, "")
                .getMessage().contains("CORS_ORIGINS"));
        assertTrue(expectFail(GOOD_JWT, GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, "  , ")
                .getMessage().contains("CORS_ORIGINS"));
    }

    @Test
    void rejectsWildcardCorsOrigins() {
        assertTrue(expectFail(GOOD_JWT, GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, "*")
                .getMessage().contains("wildcard"));
        assertTrue(expectFail(GOOD_JWT, GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, "https://jlstores.in,https://*.vercel.app")
                .getMessage().contains("wildcard"));
    }

    @Test
    void rejectsLocalhostCorsOriginsInProduction() {
        assertTrue(expectFail(GOOD_JWT, GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, "https://jlstores.in,http://localhost:5500")
                .getMessage().contains("localhost"));
        expectFail(GOOD_JWT, GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, "http://127.0.0.1:5500");
    }

    // ── Secrets must never leak into the error text ──
    @Test
    void neverEchoesSecretValuesInMessages() {
        // Distinctive values that cannot occur incidentally in the message prose.
        String secretJwt = "xY7q-jwt";                  // too short → triggers a failure
        String secretDb = "xY7q-db";
        String secretAdmin = "xY7q-admin";

        assertFalse(expectFail(secretJwt, GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, GOOD_CORS)
                .getMessage().contains(secretJwt));
        assertFalse(expectFail(GOOD_JWT, DEFAULT_DB_PASSWORD, GOOD_ADMIN_PASSWORD, GOOD_CORS)
                .getMessage().contains(DEFAULT_DB_PASSWORD));
        assertFalse(expectFail(GOOD_JWT, secretDb, "weak", GOOD_CORS).getMessage().contains(secretDb));
        // Shorter than the minimum → fails on length, and the value must not appear.
        assertFalse(expectFail(GOOD_JWT, GOOD_DB_PASSWORD, secretAdmin, GOOD_CORS)
                .getMessage().contains(secretAdmin));
    }
}
