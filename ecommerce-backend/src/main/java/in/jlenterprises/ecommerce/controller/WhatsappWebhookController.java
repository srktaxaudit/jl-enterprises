package in.jlenterprises.ecommerce.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.jlenterprises.ecommerce.notification.WhatsAppService;
import in.jlenterprises.ecommerce.service.WhatsappWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Public Meta WhatsApp webhook endpoint (no auth — Meta calls it directly).
 * GET is the one-time subscription handshake; POST receives delivery-status and inbound-message events.
 * Optional payload authenticity check via the X-Hub-Signature-256 HMAC when an app secret is configured.
 */
@RestController
@RequestMapping("/api/v1/webhooks/whatsapp")
@Tag(name = "WhatsApp Webhook", description = "Meta Cloud API callback (delivery status + inbound)")
public class WhatsappWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookController.class);

    private final WhatsAppService whatsApp;
    private final WhatsappWebhookService webhook;
    private final ObjectMapper mapper;

    public WhatsappWebhookController(WhatsAppService whatsApp, WhatsappWebhookService webhook, ObjectMapper mapper) {
        this.whatsApp = whatsApp;
        this.webhook = webhook;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Webhook verification handshake")
    public ResponseEntity<String> verify(@RequestParam(name = "hub.mode", required = false) String mode,
                                         @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
                                         @RequestParam(name = "hub.challenge", required = false) String challenge) {
        String expected = whatsApp.verifyToken();
        if ("subscribe".equals(mode) && expected != null && !expected.isBlank() && expected.equals(verifyToken)) {
            return ResponseEntity.ok(challenge == null ? "" : challenge);
        }
        log.warn("WhatsApp webhook verification rejected (mode={}, token match={})", mode,
                expected != null && expected.equals(verifyToken));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    @PostMapping
    @Operation(summary = "Receive delivery-status / inbound events")
    public ResponseEntity<String> receive(@RequestBody(required = false) String body,
                                          @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {
        // Always 200 so Meta doesn't enter a retry storm; processing errors are logged, not surfaced.
        try {
            if (!signatureValid(body, signature)) {
                log.warn("WhatsApp webhook signature check failed — payload ignored");
                return ResponseEntity.ok("EVENT_RECEIVED");
            }
            if (body != null && !body.isBlank()) {
                JsonNode payload = mapper.readTree(body);
                int applied = webhook.ingest(payload);
                if (applied > 0) log.info("WhatsApp webhook: applied {} status update(s)", applied);
            }
        } catch (Exception e) {
            log.warn("WhatsApp webhook processing error: {}", e.getMessage());
        }
        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    /** True when signatures aren't enforced (no app secret) or the HMAC matches. */
    private boolean signatureValid(String body, String signature) {
        String secret = whatsApp.appSecret();
        if (secret == null || secret.isBlank()) return true; // not enforced until an app secret is set
        if (signature == null || !signature.startsWith("sha256=")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((body == null ? "" : body).getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + toHex(digest);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("WhatsApp webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
