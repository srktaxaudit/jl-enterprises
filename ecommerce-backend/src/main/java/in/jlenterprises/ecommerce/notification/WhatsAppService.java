package in.jlenterprises.ecommerce.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Low-level transport to the Meta WhatsApp Cloud API. The only class that talks to Meta.
 * When WHATSAPP_TOKEN + WHATSAPP_PHONE_ID are set it sends for real and returns the
 * provider message id; otherwise callers run in demo mode (they simulate + log). Orchestration
 * (audiences, campaigns, logging, retries) lives in WhatsappCampaignService.
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private static final String GRAPH = "https://graph.facebook.com/v20.0/";

    private final ObjectMapper mapper;
    private final String token;
    private final String phoneId;
    private final String defaultCc;
    private final HttpClient http = HttpClient.newHttpClient();

    public WhatsAppService(ObjectMapper mapper,
                           @Value("${WHATSAPP_TOKEN:}") String token,
                           @Value("${WHATSAPP_PHONE_ID:}") String phoneId,
                           @Value("${WHATSAPP_DEFAULT_CC:91}") String defaultCc) {
        this.mapper = mapper;
        this.token = token;
        this.phoneId = phoneId;
        this.defaultCc = (defaultCc == null || defaultCc.isBlank()) ? "91" : defaultCc.replaceAll("[^0-9]", "");
    }

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

    private String post(String body) throws Exception {
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
