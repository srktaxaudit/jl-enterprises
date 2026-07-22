package in.jlenterprises.ecommerce.config;

import in.jlenterprises.ecommerce.security.RestAccessDeniedHandler;
import in.jlenterprises.ecommerce.security.RestAuthenticationEntryPoint;
import in.jlenterprises.ecommerce.security.filter.JwtAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 6 configuration: stateless JWT authentication, method-level
 * authorization ({@code @PreAuthorize}), a strict CORS allowlist, disabled CSRF
 * (safe for a token-in-header API), security headers, and JSON 401/403 handlers.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_AUTH = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-email",
            "/api/v1/auth/send-otp",
            "/api/v1/auth/verify-otp"
    };

    private static final String[] PUBLIC_DOCS = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/actuator/health", "/actuator/health/**"
    };

    private final AppProperties props;
    private final JwtAuthenticationFilter jwtFilter;
    private final RestAuthenticationEntryPoint entryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    /** Allow every {@code *.vercel.app} preview origin. OFF by default: the pattern matches ANY
        vercel.app site, not just ours, so production must never enable it implicitly. Switch on
        deliberately with APP_SECURITY_CORS_ALLOW_VERCEL_PREVIEWS=true for temporary preview testing. */
    private final boolean allowVercelPreviews;
    /** Per-IP allowance for the throttled public write endpoints, within the rolling window. */
    private final int rateLimitMax;
    private final long rateLimitWindowSeconds;

    public SecurityConfig(AppProperties props, JwtAuthenticationFilter jwtFilter,
                          RestAuthenticationEntryPoint entryPoint, RestAccessDeniedHandler accessDeniedHandler,
                          @org.springframework.beans.factory.annotation.Value(
                                  "${app.security.cors.allow-vercel-previews:false}") boolean allowVercelPreviews,
                          @org.springframework.beans.factory.annotation.Value(
                                  "${app.security.rate-limit.max:15}") int rateLimitMax,
                          @org.springframework.beans.factory.annotation.Value(
                                  "${app.security.rate-limit.window-seconds:60}") long rateLimitWindowSeconds) {
        this.props = props;
        this.jwtFilter = jwtFilter;
        this.entryPoint = entryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.allowVercelPreviews = allowVercelPreviews;
        this.rateLimitMax = rateLimitMax;
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_DOCS).permitAll()
                .requestMatchers(PUBLIC_AUTH).permitAll()
                // Meta WhatsApp webhook (GET handshake + POST events) — Meta calls it directly,
                // authenticated instead by the verify token + optional X-Hub-Signature-256.
                .requestMatchers("/api/v1/webhooks/whatsapp").permitAll()
                // Public catalog reads (products/categories/brands GETs) are opened per-controller
                // via method security; everything else requires authentication.
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/brands/**", "/api/v1/banners/**",
                        "/api/v1/orders/track", "/api/v1/coupons/active", "/api/v1/branding",
                        "/api/v1/sitemap.xml")
                    .permitAll()
                // Public service-request / contact / EMI-request submission (staff GET/PATCH still require auth)
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/api/v1/service-bookings", "/api/v1/contact", "/api/v1/emi-requests", "/api/v1/stock-alerts").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .headers(h -> h
                .frameOptions(f -> f.deny())
                .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data:; font-src 'self' data:; frame-ancestors 'none'"))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // Throttle unauthenticated write endpoints (OTP/register/reset/public forms)
            // before the JWT filter runs — prevents OTP/email bombing and form spam.
            .addFilterBefore(new in.jlenterprises.ecommerce.security.filter.RateLimitFilter(
                    rateLimitMax, rateLimitWindowSeconds), JwtAuthenticationFilter.class);

        return http.build();
    }

    // Prevent Boot from also registering the @Component JWT filter directly in the
    // servlet container — it must run only within the Spring Security chain above.
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtAutoRegistration(JwtAuthenticationFilter f) {
        FilterRegistrationBean<JwtAuthenticationFilter> reg = new FilterRegistrationBean<>(f);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // Auth uses a bearer token (Authorization header), NOT cookies — so we do
        // not need credentialed CORS. Keeping allowCredentials=false lets wildcard
        // origin PATTERNS work; browsers forbid "*"/patterns with allowCredentials=true.
        //
        // The EXACT allowed origins come from CORS_ORIGINS (app.security.cors.allowed-origins).
        // Nothing is hardcoded here: production lists its real domains in that variable, and the
        // local-development defaults live in application.yml. StartupConfigValidator rejects a
        // production CORS_ORIGINS that is missing, wildcarded, or points at localhost.
        List<String> originPatterns = new java.util.ArrayList<>(props.security().cors().allowedOrigins());
        // Vercel gives every deployment its own subdomain (each git preview is
        // jl-enterprises-git-<branch>-<team>.vercel.app). Matching "https://*.vercel.app" would
        // cover them all — but it also matches EVERY OTHER vercel.app site on the internet, so it
        // stays off unless explicitly switched on for temporary preview testing.
        if (allowVercelPreviews && originPatterns.stream().noneMatch(o -> o.contains("*.vercel.app"))) {
            originPatterns.add("https://*.vercel.app");
        }
        cfg.setAllowedOriginPatterns(originPatterns);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
