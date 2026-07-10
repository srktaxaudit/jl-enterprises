package in.jlenterprises.ecommerce.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.jlenterprises.ecommerce.entity.AppSetting;
import in.jlenterprises.ecommerce.repository.AppSettingRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Low-level transport to the Meta WhatsApp Cloud API. The only class that talks to Meta.
 * <p>
 * Credentials resolve from the {@code app_settings} table first (editable in the portal
 * Connection tab) and fall back to environment variables — so the token can be rotated
 * without a redeploy. When a token + phone id are present it sends for real and returns the
 * provider message id; otherwise callers run in demo mode (they simulate + log). Orchestration
 * (audiences, campaigns, logging, retries) lives in WhatsappCampaignService; connection checks
 * and template sync live in WhatsappConnectionService / WhatsappTemplateService.
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private static final String GRAPH = "https://graph.facebook.com/v20.0/";

    /** app_settings keys — DB overrides env so creds are editable from the portal. */
    public static final String KEY_TOKEN = "whatsapp.token";
    public static final String KEY_PHONE_ID = "whatsapp.phone_id";
    public static final String KEY_WABA_ID = "whatsapp.waba_id";
    public static final String KEY_DEFAULT_CC = "whatsapp.default_cc";
    public static final String KEY_VERIFY_TOKEN = "whatsapp.verify_token";
    public static final String KEY_APP_SECRET = "whatsapp.app_secret";

    private final ObjectMapper mapper;
    private final AppSettingRepository settings;
    private final HttpClient http = HttpClient.newHttpClient();

    private final String envToken;
    private final String envPhoneId;
    private final String envWabaId;
    private final String envVerifyToken;
    private final String envAppSecret;
    private final String envDefaultCc;

    // Resolved values (DB else env), cached in memory. Refreshed at startup and whenever
    // the Connection tab saves — avoids a DB read on every message in a large campaign.
    private volatile String token;
    private volatile String phoneId;
    private volatile String wabaId;
    private volatile String verifyToken;
    private volatile String appSecret;
    private volatile String defaultCc;
    private volatile boolean tokenFromPortal;

    public WhatsAppService(ObjectMapper mapper,
                           AppSettingRepository settings,
                           @Value("${WHATSAPP_TOKEN:}") String envToken,
                           @Value("${WHATSAPP_PHONE_ID:}") String envPhoneId,
                           @Value("${WHATSAPP_WABA_ID:}") String envWabaId,
                           @Value("${WHATSAPP_VERIFY_TOKEN:}") String envVerifyToken,
                           @Value("${WHATSAPP_APP_SECRET:}") String envAppSecret,
                           @Value("${WHATSAPP_DEFAULT_CC:91}") String envDefaultCc) {
        this.mapper = mapper;
        this.settings = settings;
        this.envToken = envToken;
        this.envPhoneId = envPhoneId;
        this.envWabaId = envWabaId;
        this.envVerifyToken = envVerifyToken;
        this.envAppSecret = envAppSecret;
        this.envDefaultCc = envDefaultCc;
        // Seed from env immediately; DB overrides are applied in reload() once JPA is ready.
        this.token = blankToNull(envToken);
        this.phoneId = blankToNull(envPhoneId);
        this.wabaId = blankToNull(envWabaId);
        this.verifyToken = blankToNull(envVerifyToken);
        this.appSecret = blankToNull(envAppSecret);
        this.defaultCc = normalizeCc(envDefaultCc);
        this.tokenFromPortal = false;
    }

    /** Re-read credentials from the DB (falling back to env). Call after saving from the portal. */
    @PostConstruct
    public void reload() {
        try {
            this.token = resolve(KEY_TOKEN, envToken);
            this.phoneId = resolve(KEY_PHONE_ID, envPhoneId);
            this.wabaId = resolve(KEY_WABA_ID, envWabaId);
            this.verifyToken = resolve(KEY_VERIFY_TOKEN, envVerifyToken);
            this.appSecret = resolve(KEY_APP_SECRET, envAppSecret);
            this.defaultCc = normalizeCc(resolve(KEY_DEFAULT_CC, envDefaultCc));
            this.tokenFromPortal = dbValue(KEY_TOKEN) != null;
        } catch (Exception e) {
            log.warn("Could not load WhatsApp settings from DB, keeping env defaults: {}", e.getMessage());
        }
    }

    private String dbValue(String key) {
        try {
            return settings.findById(key).map(AppSetting::getValue)
                    .map(String::trim).filter(v -> !v.isBlank()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolve(String key, String envFallback) {
        String v = dbValue(key);
        return v != null ? v : blankToNull(envFallback);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String normalizeCc(String cc) {
        String d = (cc == null ? "" : cc).replaceAll("[^0-9]", "");
        return d.isBlank() ? "91" : d;
    }

    // ── resolved credential accessors ──
    public String phoneId() { return phoneId; }
    public String wabaId() { return wabaId; }
    public String verifyToken() { return verifyToken; }
    /** Meta app secret, used to verify the X-Hub-Signature-256 on inbound webhooks. May be null. */
    public String appSecret() { return appSecret; }
    public String defaultCc() { return defaultCc; }
    /** True when the active token comes from the portal (app_settings) rather than env. */
    public boolean tokenFromPortal() { return tokenFromPortal; }

    /**
     * Normalise a phone to the digits-only international form Meta expects
     * (country code + number, no '+'/spaces). Bare 10-digit numbers get the
     * default country code prepended; a leading national '0' or '00' is stripped.
     */
    public String normalize(String phone) {
        if (phone == null) return null;
        String d = phone.replaceAll("[^0-9]", "");
        if (d.startsWith("00")) d = d.substring(2);
        if (d.length() == 11 && d.startsWith("0")) d = d.substring(1);
        if (d.length() == 10) d = defaultCc + d;
        return d;
    }

    /** True when Meta credentials are configured (real sends possible). */
    public boolean isConfigured() {
        return token != null && !token.isBlank() && phoneId != null && !phoneId.isBlank();
    }

    /** Free-text message — only valid inside the 24h customer-service window. Returns the message id. */
    public String sendText(String toPhone, String message) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "messaging_product", "whatsapp",
                "to", normalize(toPhone),
                "type", "text",
                "text", Map.of("body", message)));
        return post(body);
    }

    /** Approved marketing template with positional body params (e.g. the customer's name). Returns the message id. */
    public String sendTemplate(String toPhone, String templateName, String language, List<String> bodyParams) throws Exception {
        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", (language == null || language.isBlank()) ? "en" : language));
        if (bodyParams != null && !bodyParams.isEmpty()) {
            List<Map<String, String>> params = new ArrayList<>();
            for (String p : bodyParams) params.add(Map.of("type", "text", "text", p == null ? "" : p));
            template.put("components", List.of(Map.of("type", "body", "parameters", params)));
        }
        String body = mapper.writeValueAsString(Map.of(
                "messaging_product", "whatsapp",
                "to", normalize(toPhone),
                "type", "template",
                "template", template));
        return post(body);
    }

    // ── Meta reads (Connection tab + template sync) ──

    /** Phone-number node: verified_name, display_phone_number, quality_rating, code/name status. */
    public JsonNode phoneInfo() throws Exception {
        requireConfigured();
        return graphGet(GRAPH + phoneId
                + "?fields=verified_name,display_phone_number,quality_rating,code_verification_status,name_status");
    }

    /** All message templates on the configured WABA (paginated). Empty when no WABA id is set. */
    public List<JsonNode> fetchTemplates() throws Exception {
        requireConfigured();
        List<JsonNode> out = new ArrayList<>();
        if (wabaId == null || wabaId.isBlank()) return out;
        String url = GRAPH + wabaId + "/message_templates?fields=name,status,language,category,components&limit=100";
        int guard = 0;
        while (url != null && guard++ < 25) {
            JsonNode root = graphGet(url);
            JsonNode data = root.get("data");
            if (data != null && data.isArray()) data.forEach(out::add);
            JsonNode next = root.path("paging").path("next");
            url = (next.isMissingNode() || next.isNull()) ? null : next.asText();
        }
        return out;
    }

    /** Detect the WABA id this token can manage, via the token-debug endpoint. Null if none found. */
    public String detectWabaId() throws Exception {
        if (token == null || token.isBlank()) return null;
        String enc = URLEncoder.encode(token, StandardCharsets.UTF_8);
        JsonNode root = graphGet(GRAPH + "debug_token?input_token=" + enc + "&access_token=" + enc);
        JsonNode scopes = root.path("data").path("granular_scopes");
        if (scopes.isArray()) {
            for (JsonNode s : scopes) {
                if (s.path("scope").asText("").contains("whatsapp_business")) {
                    JsonNode ids = s.path("target_ids");
                    if (ids.isArray() && !ids.isEmpty()) return ids.get(0).asText();
                }
            }
        }
        return null;
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("WhatsApp is not configured — a token and phone-number id are required.");
        }
    }

    private JsonNode graphGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new IllegalStateException("WhatsApp API " + res.statusCode() + ": " + res.body());
        }
        return mapper.readTree(res.body());
    }

    private String post(String body) throws Exception {
        requireConfigured();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GRAPH + phoneId + "/messages"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            throw new IllegalStateException("WhatsApp API " + res.statusCode() + ": " + res.body());
        }
        try {
            JsonNode msgs = mapper.readTree(res.body()).get("messages");
            if (msgs != null && msgs.isArray() && !msgs.isEmpty()) {
                JsonNode id = msgs.get(0).get("id");
                if (id != null) return id.asText();
            }
        } catch (Exception ignore) {
            log.warn("Could not parse WhatsApp message id from response");
        }
        return null;
    }
}
