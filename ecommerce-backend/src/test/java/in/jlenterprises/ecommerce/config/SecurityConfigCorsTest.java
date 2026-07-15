package in.jlenterprises.ecommerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * CORS policy: exact origins come from CORS_ORIGINS, and *.vercel.app preview origins are
 * only ever allowed when explicitly switched on.
 */
class SecurityConfigCorsTest {

    private static final List<String> PROD_ORIGINS = List.of("https://jlstores.in", "https://www.jlstores.in");

    /** Build the CORS config the app would use for the given origins + preview flag. */
    private CorsConfiguration corsFor(List<String> allowedOrigins, boolean allowVercelPreviews) {
        var cors = new AppProperties.Security.Cors(allowedOrigins);
        var security = new AppProperties.Security(cors, null);
        var props = new AppProperties(null, security, null, null, null, null);
        // Only props + the preview flag are used by corsConfigurationSource(); the filter-chain
        // collaborators are irrelevant here.
        var config = new SecurityConfig(props, null, null, null, allowVercelPreviews, 15, 60);
        var source = (UrlBasedCorsConfigurationSource) config.corsConfigurationSource();
        CorsConfiguration cfg = source.getCorsConfigurations().get("/**");
        assertNotNull(cfg, "CORS config should be registered for /**");
        return cfg;
    }

    @Test
    void rejectsArbitraryVercelPreviewOriginsByDefault() {
        CorsConfiguration cfg = corsFor(PROD_ORIGINS, false);

        // Someone else's vercel.app site, and a preview-looking one — both refused.
        assertNull(cfg.checkOrigin("https://totally-unrelated.vercel.app"));
        assertNull(cfg.checkOrigin("https://jl-enterprises-git-main-someone.vercel.app"));
    }

    @Test
    void acceptsExactConfiguredOrigins() {
        CorsConfiguration cfg = corsFor(PROD_ORIGINS, false);

        assertEquals("https://jlstores.in", cfg.checkOrigin("https://jlstores.in"));
        assertEquals("https://www.jlstores.in", cfg.checkOrigin("https://www.jlstores.in"));
    }

    @Test
    void rejectsOriginsThatAreNotConfigured() {
        CorsConfiguration cfg = corsFor(PROD_ORIGINS, false);

        assertNull(cfg.checkOrigin("https://evil.example.com"));
        // Not configured => not allowed, even though it looks like our domain.
        assertNull(cfg.checkOrigin("https://jlstores.in.evil.example.com"));
    }

    @Test
    void acceptsVercelPreviewsOnlyWhenExplicitlyEnabled() {
        CorsConfiguration enabled = corsFor(PROD_ORIGINS, true);

        assertNotNull(enabled.checkOrigin("https://jl-enterprises-git-main-someone.vercel.app"));
        // The exact production origins still work with previews on.
        assertEquals("https://jlstores.in", enabled.checkOrigin("https://jlstores.in"));
    }

    @Test
    void productionOriginsAreNotHardcoded() {
        // With only a local origin configured, the production domain must NOT be allowed —
        // proving nothing is baked into SecurityConfig.
        CorsConfiguration cfg = corsFor(List.of("http://localhost:5500"), false);

        assertNull(cfg.checkOrigin("https://jlstores.in"));
        assertEquals("http://localhost:5500", cfg.checkOrigin("http://localhost:5500"));
    }

    @Test
    void doesNotUseCredentialedCors() {
        // allowCredentials must stay false: origin PATTERNS are only legal without credentials,
        // and the API authenticates with a bearer header rather than cookies.
        assertEquals(Boolean.FALSE, corsFor(PROD_ORIGINS, false).getAllowCredentials());
    }
}
