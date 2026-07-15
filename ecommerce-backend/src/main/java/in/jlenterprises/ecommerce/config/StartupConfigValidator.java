package in.jlenterprises.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Refuses to boot a real deployment on unsafe development defaults.
 *
 * <p>"Production" is the explicit {@code prod} Spring profile (set by render.yaml via
 * SPRING_PROFILES_ACTIVE) OR — as a deliberate fallback — any non-local datasource, so a
 * deployment whose profile was never set still gets the checks. Local development (localhost
 * DB, no prod profile) only logs warnings, so the built-in defaults keep working.
 *
 * <p>Messages never contain a secret VALUE — they only name the variable to fix.
 * Mirrors the bootstrap-admin fail-fast in {@link DataInitializer}.
 */
@Component
public class StartupConfigValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupConfigValidator.class);

    /** Must match the dev fallbacks in application.yml. */
    static final String DEFAULT_JWT_SECRET = "dev-only-change-me-a-long-random-secret-at-least-32-bytes";
    static final String DEFAULT_DB_PASSWORD = "jl";
    /** HS256 signing needs a key of at least 256 bits. */
    static final int MIN_JWT_SECRET_BYTES = 32;
    /** Minimum length for the bootstrap super-admin password. */
    static final int MIN_ADMIN_PASSWORD_LENGTH = 12;
    /** Obvious values that must never guard a production super-admin (compared lower-cased). */
    static final Set<String> WEAK_ADMIN_PASSWORDS = Set.of(
            "admin@12345", "admin", "admin123", "administrator", "password", "password123",
            "passw0rd", "changeme", "letmein", "12345678", "123456789", "qwerty", "jl", "jladmin");

    private final Environment environment;
    private final String jwtSecret;
    private final String dbUrl;
    private final String dbPassword;
    private final String adminPassword;
    private final String corsOrigins;
    private final boolean allowVercelPreviews;

    public StartupConfigValidator(Environment environment,
                                  @Value("${app.jwt.secret:}") String jwtSecret,
                                  @Value("${spring.datasource.url:}") String dbUrl,
                                  @Value("${spring.datasource.password:}") String dbPassword,
                                  @Value("${app.bootstrap.admin-password:}") String adminPassword,
                                  @Value("${app.security.cors.allowed-origins:}") String corsOrigins,
                                  @Value("${app.security.cors.allow-vercel-previews:false}") boolean allowVercelPreviews) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.dbUrl = dbUrl;
        this.dbPassword = dbPassword;
        this.adminPassword = adminPassword;
        this.corsOrigins = corsOrigins;
        this.allowVercelPreviews = allowVercelPreviews;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProduction()) {
            if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
                log.warn("Using the DEFAULT dev JWT secret. Set JWT_SECRET before deploying anywhere real.");
            }
            if (DEFAULT_DB_PASSWORD.equals(dbPassword)) {
                log.warn("Using the DEFAULT dev DB password. Set DB_PASSWORD before deploying anywhere real.");
            }
            return;
        }

        validateProduction(jwtSecret, dbPassword, adminPassword, corsOrigins);

        // Enabling previews requires an explicit env var (the default is false), so reaching here
        // is a deliberate, temporary choice — but it is risky enough to shout about every boot.
        if (allowVercelPreviews) {
            log.warn("CORS: *.vercel.app preview origins are ENABLED in production "
                    + "(APP_SECURITY_CORS_ALLOW_VERCEL_PREVIEWS=true). That lets ANY vercel.app site call this "
                    + "API from a browser. Unset it as soon as preview testing is done.");
        }
    }

    /** A real deployment: the explicit {@code prod} profile, or a non-local datasource. */
    private boolean isProduction() {
        if (environment.acceptsProfiles(Profiles.of("prod"))) return true;
        return !(dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1"));
    }

    /**
     * Fail-fast configuration checks for a real deployment. Package-private and static so it can be
     * unit-tested without a Spring context. Never echoes a secret value — only names the variable.
     *
     * @throws IllegalStateException on the first unsafe value found
     */
    static void validateProduction(String jwtSecret, String dbPassword, String adminPassword, String corsOrigins) {
        // ── JWT signing secret ──
        if (isBlank(jwtSecret)) {
            throw fail("JWT_SECRET is not set. Generate a strong, unique value of at least "
                    + MIN_JWT_SECRET_BYTES + " bytes and redeploy.");
        }
        if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            throw fail("JWT_SECRET is the built-in development default, so anyone could forge tokens. "
                    + "Set a strong, unique JWT_SECRET (at least " + MIN_JWT_SECRET_BYTES + " bytes) and redeploy.");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            throw fail("JWT_SECRET is too short — HS256 needs at least " + MIN_JWT_SECRET_BYTES
                    + " bytes. Set a longer random value and redeploy.");
        }

        // ── Database password ──
        if (isBlank(dbPassword)) {
            throw fail("DB_PASSWORD is not set. Set the real database password and redeploy.");
        }
        if (DEFAULT_DB_PASSWORD.equals(dbPassword)) {
            throw fail("DB_PASSWORD is the built-in development default. Set the real database password and redeploy.");
        }

        // ── Bootstrap super-admin password ──
        // A loud WARNING, not a boot failure: this value is only ever USED when seeding a brand-new
        // empty database, and DataInitializer already hard-fails that exact case on a default value.
        // An established production database must not be taken offline over a variable that is
        // never read again — but the warning fires every boot until it is fixed.
        String adminProblem = adminPasswordProblem(adminPassword);
        if (adminProblem != null) {
            log.warn("Unsafe production configuration (non-fatal): {} It is only used when seeding a "
                    + "fresh database — DataInitializer refuses to seed one with a default value — but "
                    + "set a strong value anyway to silence this warning.", adminProblem);
        }

        // ── CORS allow-list (origins are not secrets, so they may appear in the message) ──
        if (isBlank(corsOrigins)) {
            throw fail("CORS_ORIGINS is not set. List the exact browser origins allowed to call this API "
                    + "(e.g. https://jlstores.in,https://www.jlstores.in) and redeploy.");
        }
        List<String> origins = Arrays.stream(corsOrigins.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (origins.isEmpty()) {
            throw fail("CORS_ORIGINS is empty. List the exact browser origins allowed to call this API and redeploy.");
        }
        for (String origin : origins) {
            if (origin.contains("*")) {
                throw fail("CORS_ORIGINS contains the wildcard entry '" + origin + "', which would let untrusted "
                        + "sites call this API from a browser. List exact origins instead and redeploy.");
            }
            String lower = origin.toLowerCase(Locale.ROOT);
            if (lower.contains("localhost") || lower.contains("127.0.0.1")) {
                throw fail("CORS_ORIGINS contains the local development origin '" + origin
                        + "'. Remove it from the production configuration and redeploy.");
            }
        }
    }

    /** Why the bootstrap admin password is unsafe, or {@code null} if it is fine. Never echoes the value. */
    static String adminPasswordProblem(String adminPassword) {
        if (isBlank(adminPassword)) {
            return "APP_BOOTSTRAP_ADMIN_PASSWORD is not set.";
        }
        if (WEAK_ADMIN_PASSWORDS.contains(adminPassword.toLowerCase(Locale.ROOT))) {
            return "APP_BOOTSTRAP_ADMIN_PASSWORD is a well-known default/weak value.";
        }
        if (adminPassword.length() < MIN_ADMIN_PASSWORD_LENGTH) {
            return "APP_BOOTSTRAP_ADMIN_PASSWORD is too short (use at least "
                    + MIN_ADMIN_PASSWORD_LENGTH + " characters).";
        }
        return null;
    }

    private static IllegalStateException fail(String problem) {
        return new IllegalStateException("Refusing to boot — unsafe production configuration: " + problem);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
