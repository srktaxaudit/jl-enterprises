package in.jlenterprises.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/** Strongly-typed binding of the {@code app.*} settings from application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Security security,
        Otp otp,
        Mail mail
) {
    public record Jwt(
            String secret,
            Duration accessTokenTtl,
            Duration refreshTokenTtl,
            Duration rememberMeRefreshTtl,
            String issuer
    ) {}

    public record Security(Cors cors, Login login) {
        public record Cors(List<String> allowedOrigins) {}
        public record Login(int maxFailedAttempts, Duration lockDuration) {}
    }

    public record Otp(int length, Duration ttl, int maxAttempts) {}

    public record Mail(String from, String verificationBaseUrl) {}
}
