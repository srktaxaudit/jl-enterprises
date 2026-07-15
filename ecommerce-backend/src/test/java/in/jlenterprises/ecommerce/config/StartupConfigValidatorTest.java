package in.jlenterprises.ecommerce.config;

import org.junit.jupiter.api.Test;

import static in.jlenterprises.ecommerce.config.StartupConfigValidator.DEFAULT_DB_PASSWORD;
import static in.jlenterprises.ecommerce.config.StartupConfigValidator.DEFAULT_JWT_SECRET;
import static in.jlenterprises.ecommerce.config.StartupConfigValidator.validateProduction;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    // Flagged (loud warning) but NOT a boot failure: the value is only used when seeding a fresh
    // database, and DataInitializer already hard-fails that case. An established production DB
    // must not be taken offline over it — this bit the very first prod deploy of these checks.
    @Test
    void weakAdminPasswordWarnsButDoesNotBlockBoot() {
        assertDoesNotThrow(() -> validateProduction(GOOD_JWT, GOOD_DB_PASSWORD, "", GOOD_CORS));
        assertDoesNotThrow(() -> validateProduction(
                GOOD_JWT, GOOD_DB_PASSWORD, DataInitializer.DEFAULT_ADMIN_PASSWORD, GOOD_CORS));
        assertDoesNotThrow(() -> validateProduction(GOOD_JWT, GOOD_DB_PASSWORD, "ChangeMe", GOOD_CORS));
        assertDoesNotThrow(() -> validateProduction(GOOD_JWT, GOOD_DB_PASSWORD, "Sh0rt!", GOOD_CORS));
    }

    @Test
    void adminPasswordProblemsAreStillDetectedAndNeverEchoTheValue() {
        assertNull(StartupConfigValidator.adminPasswordProblem(GOOD_ADMIN_PASSWORD));

        String[] bad = {"", DataInitializer.DEFAULT_ADMIN_PASSWORD, "ChangeMe", "xY7q-admin"};
        for (String pw : bad) {
            String problem = StartupConfigValidator.adminPasswordProblem(pw);
            assertNotNull(problem, "should flag: <" + pw + ">");
            assertTrue(problem.contains("APP_BOOTSTRAP_ADMIN_PASSWORD"));
            if (!pw.isEmpty()) {
                assertFalse(problem.contains(pw), "warning must not echo the value");
            }
        }
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

        assertFalse(expectFail(secretJwt, GOOD_DB_PASSWORD, GOOD_ADMIN_PASSWORD, GOOD_CORS)
                .getMessage().contains(secretJwt));
        assertFalse(expectFail(GOOD_JWT, DEFAULT_DB_PASSWORD, GOOD_ADMIN_PASSWORD, GOOD_CORS)
                .getMessage().contains(DEFAULT_DB_PASSWORD));
        // Fail on CORS while a distinctive DB secret is present — it must not leak into the message.
        assertFalse(expectFail(GOOD_JWT, secretDb, GOOD_ADMIN_PASSWORD, "")
                .getMessage().contains(secretDb));
        // Admin-password warnings are covered by adminPasswordProblemsAreStillDetectedAndNeverEchoTheValue.
    }
}
