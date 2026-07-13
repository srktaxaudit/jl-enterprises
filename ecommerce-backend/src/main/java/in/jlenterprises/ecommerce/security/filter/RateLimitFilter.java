package in.jlenterprises.ecommerce.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A lightweight per-IP fixed-window rate limiter for the unauthenticated write endpoints
 * (OTP/email send, registration, password reset, public form submissions). These have no
 * other throttle, so without this they can be used for OTP/email bombing or spam.
 *
 * In-memory only (single instance) — enough for the current single-node deployment; a
 * distributed limiter (Redis) would be the next step if the API is scaled out.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    /** Only these POST paths are throttled; everything else passes straight through. */
    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/send-otp",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/contact",
            "/api/v1/emi-requests",
            "/api/v1/stock-alerts",
            "/api/v1/service-bookings");

    private final int maxPerWindow;
    private final long windowSeconds;
    private final Map<String, Window> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(int maxPerWindow, long windowSeconds) {
        this.maxPerWindow = maxPerWindow;
        this.windowSeconds = windowSeconds;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(HttpMethod.POST.matches(request.getMethod()) && LIMITED_PATHS.contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = clientIp(request) + " " + request.getRequestURI();
        long now = Instant.now().getEpochSecond();
        Window w = counters.compute(key, (k, existing) -> {
            if (existing == null || now - existing.startEpoch >= windowSeconds) {
                return new Window(now);
            }
            return existing;
        });
        if (w.count.incrementAndGet() > maxPerWindow) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please wait a minute and try again.\"}");
            return;
        }
        // Opportunistic cleanup so the map can't grow unbounded on a long-running instance.
        if (counters.size() > 10_000) {
            counters.entrySet().removeIf(e -> now - e.getValue().startEpoch >= windowSeconds);
        }
        chain.doFilter(request, response);
    }

    /** Real client IP behind Render's proxy (X-Forwarded-For: client, proxy1, …). */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        final long startEpoch;
        final AtomicInteger count = new AtomicInteger(0);
        Window(long startEpoch) { this.startEpoch = startEpoch; }
    }
}
