package in.jlenterprises.ecommerce.payment.impl;

import in.jlenterprises.ecommerce.config.AppProperties;
import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.payment.PaymentConfirmation;
import in.jlenterprises.ecommerce.payment.PaymentInitResult;
import in.jlenterprises.ecommerce.payment.PaymentStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Razorpay integration (no SDK — uses the Orders REST API + JDK HMAC so we add no
 * dependency). {@link #initiate} creates a Razorpay order and returns its id (as the
 * provider reference) plus the public key id (as clientData) for the browser checkout.
 * {@link #verify} recomputes {@code HMAC_SHA256(order_id|payment_id, key_secret)} and
 * constant-time compares it to the signature Razorpay returned.
 */
@Component
public class RazorpayPaymentStrategy implements PaymentStrategy {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentStrategy.class);

    private final AppProperties.Razorpay cfg;
    private final RestClient http = in.jlenterprises.ecommerce.util.Http.client();

    public RazorpayPaymentStrategy(AppProperties props) {
        this.cfg = props.razorpay();
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.RAZORPAY;
    }

    @Override
    public PaymentInitResult initiate(Payment payment) {
        if (cfg == null || !cfg.configured()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Online payment is not available right now. Please choose Cash on Delivery.");
        }
        long paise = payment.getAmount().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String basic = Base64.getEncoder()
                .encodeToString((cfg.keyId() + ":" + cfg.keySecret()).getBytes(StandardCharsets.UTF_8));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = http.post()
                    .uri("https://api.razorpay.com/v1/orders")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "amount", paise,
                            "currency", payment.getCurrency() == null ? "INR" : payment.getCurrency(),
                            "receipt", payment.getOrder().getOrderNumber(),
                            "payment_capture", 1))
                    .retrieve()
                    .body(Map.class);
            String razorpayOrderId = resp == null ? null : (String) resp.get("id");
            if (razorpayOrderId == null) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY, "Could not start the online payment. Please try again.");
            }
            // provider reference = Razorpay order id (persisted by PaymentServiceImpl);
            // clientData = public key id the browser needs to open Razorpay Checkout.
            return new PaymentInitResult("razorpay", razorpayOrderId, cfg.keyId(), PaymentStatus.PENDING);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Razorpay order creation failed for {}: {}", payment.getOrder().getOrderNumber(), e.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Could not start the online payment. Please try again.");
        }
    }

    @Override
    public boolean verify(Payment payment, PaymentConfirmation confirmation) {
        if (cfg == null || !cfg.configured() || confirmation == null
                || confirmation.providerReference() == null || confirmation.signature() == null) {
            return false;
        }
        String razorpayOrderId = payment.getProviderPaymentId();   // stored during initiate
        String razorpayPaymentId = confirmation.providerReference();
        String expected = hmacSha256Hex(razorpayOrderId + "|" + razorpayPaymentId, cfg.keySecret());
        return expected != null && constantTimeEquals(expected, confirmation.signature());
    }

    @Override
    public String refund(Payment payment) {
        if (cfg == null || !cfg.configured()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Online payments are not configured — refund manually in the Razorpay dashboard.");
        }
        // The captured payment id (pay_...) lives on the CHARGE transaction recorded at
        // confirm/webhook time; payment.providerPaymentId holds the ORDER id (order_...).
        String payId = payment.getTransactions().stream()
                .filter(t -> t.getType() == in.jlenterprises.ecommerce.constant.TransactionType.CHARGE
                        && t.getTransactionStatus() == PaymentStatus.SUCCESS
                        && t.getProviderReference() != null
                        && t.getProviderReference().startsWith("pay_"))
                .map(t -> t.getProviderReference())
                .reduce((first, second) -> second)   // latest
                .orElse(null);
        if (payId == null) {
            throw new BusinessException("No captured Razorpay payment reference found for this order — "
                    + "refund manually in the Razorpay dashboard.");
        }
        long paise = payment.getAmount().movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = http.post()
                    .uri("https://api.razorpay.com/v1/payments/" + payId + "/refund")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("amount", paise))
                    .retrieve()
                    .body(Map.class);
            String refundId = resp == null ? null : (String) resp.get("id");
            if (refundId == null) {
                throw new BusinessException(HttpStatus.BAD_GATEWAY,
                        "Razorpay did not confirm the refund. Check the Razorpay dashboard before retrying.");
            }
            log.info("Razorpay refund {} issued for payment {} (order {})",
                    refundId, payId, payment.getOrder().getOrderNumber());
            return refundId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Razorpay refund failed for {}: {}", payment.getOrder().getOrderNumber(), e.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY,
                    "The Razorpay refund failed (" + e.getMessage() + "). Nothing was changed — "
                    + "retry, or refund manually in the Razorpay dashboard.");
        }
    }

    @Override
    public String capturedPaymentId(Payment payment) {
        if (cfg == null || !cfg.configured() || payment.getProviderPaymentId() == null
                || !payment.getProviderPaymentId().startsWith("order_")) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = http.get()
                    .uri("https://api.razorpay.com/v1/orders/" + payment.getProviderPaymentId() + "/payments")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth())
                    .retrieve()
                    .body(Map.class);
            Object items = resp == null ? null : resp.get("items");
            if (items instanceof java.util.List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> m && "captured".equals(m.get("status"))) {
                        return (String) m.get("id");
                    }
                }
            }
            return null;
        } catch (Exception e) {
            // Reconciliation is best-effort: on doubt report "not captured" and let the
            // caller be CONSERVATIVE (i.e. don't cancel — try again next sweep).
            log.warn("Razorpay payment lookup failed for {}: {}", payment.getProviderPaymentId(), e.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Razorpay lookup failed");
        }
    }

    private String basicAuth() {
        return Base64.getEncoder()
                .encodeToString((cfg.keyId() + ":" + cfg.keySecret()).getBytes(StandardCharsets.UTF_8));
    }

    /** Shared with the webhook controller (same HMAC-SHA256-hex scheme). */
    public static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
