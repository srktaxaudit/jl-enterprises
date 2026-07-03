package in.jlenterprises.ecommerce.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.jlenterprises.ecommerce.dto.admin.BroadcastResult;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Sends WhatsApp messages via the Meta Cloud API when credentials are set
 * (WHATSAPP_TOKEN + WHATSAPP_PHONE_ID). Without them it runs in demo mode:
 * recipients are logged, nothing is sent — so the feature is safe out of the box.
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private static final String GRAPH = "https://graph.facebook.com/v20.0/";

    private final UserRepository userRepository;
    private final ObjectMapper mapper;
    private final String token;
    private final String phoneId;
    private final HttpClient http = HttpClient.newHttpClient();

    public WhatsAppService(UserRepository userRepository, ObjectMapper mapper,
                           @Value("${WHATSAPP_TOKEN:}") String token,
                           @Value("${WHATSAPP_PHONE_ID:}") String phoneId) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.token = token;
        this.phoneId = phoneId;
    }

    private boolean configured() {
        return token != null && !token.isBlank() && phoneId != null && !phoneId.isBlank();
    }

    @Transactional(readOnly = true)
    public BroadcastResult broadcast(String message, boolean onlyVerified) {
        List<User> recipients = userRepository.findByPhoneNotNull().stream()
                .filter(u -> !onlyVerified || u.isPhoneVerified())
                .toList();

        boolean demo = !configured();
        int sent = 0, failed = 0;
        for (User user : recipients) {
            try {
                if (demo) {
                    log.info("[WhatsApp DEMO] would send to {}: {}", user.getPhone(), message);
                } else {
                    sendText(user.getPhone(), message);
                }
                sent++;
            } catch (Exception e) {
                failed++;
                log.warn("WhatsApp send failed for {}", user.getPhone(), e);
            }
        }
        return new BroadcastResult(recipients.size(), sent, failed, demo);
    }

    private void sendText(String toPhone, String message) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "messaging_product", "whatsapp",
                "to", toPhone,
                "type", "text",
                "text", Map.of("body", message)));
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
    }
}
