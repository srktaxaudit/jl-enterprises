package in.jlenterprises.ecommerce.security;

import in.jlenterprises.ecommerce.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, IP+identifier sliding-window throttle for the login endpoint —
 * a first line of brute-force defence complementing the per-account lockout in
 * {@code AuthService}. For multi-instance deployments, back this with Redis.
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration window;
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public LoginAttemptService(AppProperties props) {
        this.maxAttempts = props.security().login().maxFailedAttempts();
        this.window = props.security().login().lockDuration();
    }

    public boolean isBlocked(String key) {
        Deque<Instant> attempts = failures.get(key);
        if (attempts == null) return false;
        synchronized (attempts) {
            prune(attempts);
            return attempts.size() >= maxAttempts;
        }
    }

    public void recordFailure(String key) {
        Deque<Instant> attempts = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts);
            attempts.addLast(Instant.now());
        }
    }

    public void reset(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Instant> attempts) {
        Instant cutoff = Instant.now().minus(window);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.pollFirst();
        }
    }
}
