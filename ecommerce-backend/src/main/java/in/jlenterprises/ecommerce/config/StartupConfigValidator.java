package in.jlenterprises.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Guards against booting a real deployment on the built-in development defaults.
 * A public default JWT secret means anyone could forge tokens; default DB creds are
 * equally unsafe. We only fail-fast when the datasource is NOT local (i.e. a genuine
 * remote/prod database) so local development on the defaults keeps working — it just
 * logs a loud warning. Mirrors the bootstrap-admin fail-fast in {@link DataInitializer}.
 */
@Component
public class StartupConfigValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupConfigValidator.class);

    /** Must match the dev fallbacks in application.yml. */
    static final String DEFAULT_JWT_SECRET = "dev-only-change-me-a-long-random-secret-at-least-32-bytes";
    static final String DEFAULT_DB_PASSWORD = "jl";

    private final String jwtSecret;
    private final String dbUrl;
    private final String dbPassword;

    public StartupConfigValidator(@Value("${app.jwt.secret:}") String jwtSecret,
                                  @Value("${spring.datasource.url:}") String dbUrl,
                                  @Value("${spring.datasource.password:}") String dbPassword) {
        this.jwtSecret = jwtSecret;
        this.dbUrl = dbUrl;
        this.dbPassword = dbPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean localDb = dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1");
        boolean defaultJwt = DEFAULT_JWT_SECRET.equals(jwtSecret);
        boolean defaultDbPass = DEFAULT_DB_PASSWORD.equals(dbPassword);

        if (localDb) {
            if (defaultJwt) log.warn("Using the DEFAULT dev JWT secret. Set JWT_SECRET before deploying anywhere real.");
            if (defaultDbPass) log.warn("Using the DEFAULT dev DB password. Set DB_PASSWORD before deploying anywhere real.");
            return;
        }

        // Non-local datasource → treat as a real deployment; refuse to boot on public defaults.
        if (defaultJwt) {
            throw new IllegalStateException(
                    "Refusing to boot: JWT_SECRET is the built-in development default, so tokens would be "
                    + "forgeable. Set a strong, unique JWT_SECRET (>=32 bytes) and redeploy.");
        }
        if (defaultDbPass) {
            throw new IllegalStateException(
                    "Refusing to boot: DB_PASSWORD is the built-in development default. Set the real "
                    + "database password (DB_PASSWORD) and redeploy.");
        }
    }
}
