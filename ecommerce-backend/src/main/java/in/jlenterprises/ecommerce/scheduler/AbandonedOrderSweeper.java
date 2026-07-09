package in.jlenterprises.ecommerce.scheduler;

import in.jlenterprises.ecommerce.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Periodically releases stock held by abandoned online orders — ones placed with a
 * gateway (non-COD) method that were never paid and are still PENDING past a grace
 * window. Without this, an abandoned Razorpay checkout leaves its stock deducted
 * forever (phantom out-of-stock). COD orders are never touched (they stay PENDING
 * legitimately until delivery — see {@code OrderRepository.findAbandonedOnlineOrders}).
 *
 * Currently a no-op while the store is COD-only; it takes effect once online payments
 * are live. Toggle with {@code app.orders.abandon-sweep.enabled}; tune the grace window
 * and cadence with {@code grace-minutes} / {@code interval-ms}.
 */
@Component
public class AbandonedOrderSweeper {

    private static final Logger log = LoggerFactory.getLogger(AbandonedOrderSweeper.class);

    private final OrderService orderService;
    private final boolean enabled;
    private final long graceMinutes;

    public AbandonedOrderSweeper(OrderService orderService,
                                 @Value("${app.orders.abandon-sweep.enabled:true}") boolean enabled,
                                 @Value("${app.orders.abandon-sweep.grace-minutes:30}") long graceMinutes) {
        this.orderService = orderService;
        this.enabled = enabled;
        this.graceMinutes = graceMinutes;
    }

    // Default: run every 10 minutes (after a 2-minute startup delay).
    @Scheduled(fixedDelayString = "${app.orders.abandon-sweep.interval-ms:600000}", initialDelay = 120_000)
    public void sweep() {
        if (!enabled) return;
        try {
            int released = orderService.expireAbandonedOrders(Duration.ofMinutes(graceMinutes));
            if (released > 0) log.info("Released stock for {} abandoned online order(s)", released);
        } catch (Exception e) {
            // Best-effort background task — never let a failure escape the scheduler thread.
            log.warn("Abandoned-order sweep failed: {}", e.getMessage());
        }
    }
}
