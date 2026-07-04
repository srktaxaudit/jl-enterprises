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
        Mail mail,
        Supabase supabase,
        Razorpay razorpay
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

    public record Mail(String from, String verificationBaseUrl, String passwordResetBaseUrl) {}

    /** Supabase Storage — product image uploads. Blank url/serviceKey = uploads disabled. */
    public record Supabase(String url, String serviceKey, String bucket) {
        public boolean configured() {
            return url != null && !url.isBlank() && serviceKey != null && !serviceKey.isBlank();
        }
    }

    /** Razorpay online payments. Blank keys = COD-only (Razorpay initiate is rejected clearly). */
    public record Razorpay(String keyId, String keySecret) {
        public boolean configured() {
            return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
        }
    }
}
