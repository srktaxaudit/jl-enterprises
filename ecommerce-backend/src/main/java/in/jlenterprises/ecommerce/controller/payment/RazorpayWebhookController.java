package in.jlenterprises.ecommerce.controller.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.jlenterprises.ecommerce.config.AppProperties;
import in.jlenterprises.ecommerce.payment.impl.RazorpayPaymentStrategy;
import in.jlenterprises.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Razorpay server-to-server webhook. This is the DURABLE settlement path: the customer
 * confirm callback comes from their browser, which can die right after payment — the
 * webhook arrives regardless, so a captured payment can never be lost (and never
 * auto-cancelled by the abandoned-order sweeper).
 *
 * <p>Authenticated by {@code X-Razorpay-Signature} = HMAC-SHA256(raw body, webhook
 * secret). Until RAZORPAY_WEBHOOK_SECRET is set, every request is rejected — set it to
 * the secret entered in Razorpay dashboard → Settings → Webhooks when creating the
 * webhook for {@code /api/v1/webhooks/razorpay} (event: payment.captured).
 */
@RestController
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final AppProperties.Razorpay cfg;
    private final PaymentService paymentService;
    private final ObjectMapper mapper;

    public RazorpayWebhookController(AppProperties props, PaymentService paymentService, ObjectMapper mapper) {
        this.cfg = props.razorpay();
        this.paymentService = paymentService;
        this.mapper = mapper;
    }

    @PostMapping("/api/v1/webhooks/razorpay")
    @Operation(summary = "Razorpay payment webhook (signature-verified)")
    public ResponseEntity<String> handle(@RequestBody String rawBody,
                                         @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        if (cfg == null || !cfg.webhookConfigured()) {
            // Not set up yet — refuse rather than accept unauthenticated events.
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("webhook not configured");
        }
        String expected = RazorpayPaymentStrategy.hmacSha256Hex(rawBody, cfg.webhookSecret());
        if (signature == null || expected == null
                || !RazorpayPaymentStrategy.constantTimeEquals(expected, signature)) {
            log.warn("Razorpay webhook rejected: bad signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
        }

        try {
            JsonNode root = mapper.readTree(rawBody);
            String event = root.path("event").asText("");
            if ("payment.captured".equals(event)) {
                JsonNode pay = root.path("payload").path("payment").path("entity");
                String orderId = pay.path("order_id").asText(null);
                String paymentId = pay.path("id").asText(null);
                if (orderId != null && paymentId != null) {
                    paymentService.recordGatewayCapture(orderId, paymentId);
                }
            }
            // Every other event type is acknowledged and ignored (Razorpay retries non-2xx).
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            // 500 → Razorpay retries later; the handler is idempotent so retries are safe.
            log.error("Razorpay webhook processing failed: {}", e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error");
        }
    }
}
