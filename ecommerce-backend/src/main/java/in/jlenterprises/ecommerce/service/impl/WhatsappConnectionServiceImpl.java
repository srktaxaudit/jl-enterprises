package in.jlenterprises.ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import in.jlenterprises.ecommerce.dto.whatsapp.ConnectionStatusDto;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.notification.WhatsAppService;
import in.jlenterprises.ecommerce.request.whatsapp.ConnectionRequest;
import in.jlenterprises.ecommerce.service.SettingService;
import in.jlenterprises.ecommerce.service.WhatsappConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WhatsappConnectionServiceImpl implements WhatsappConnectionService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappConnectionServiceImpl.class);

    private final WhatsAppService whatsApp;
    private final SettingService settings;

    public WhatsappConnectionServiceImpl(WhatsAppService whatsApp, SettingService settings) {
        this.whatsApp = whatsApp;
        this.settings = settings;
    }

    @Override
    public ConnectionStatusDto status() {
        if (!whatsApp.isConfigured()) {
            return new ConnectionStatusDto(false, false, "none", whatsApp.phoneId(),
                    null, null, null, null, whatsApp.wabaId(), null,
                    "Not configured — save a token and phone-number id to go live. Campaigns run in demo mode until then.");
        }
        String source = whatsApp.tokenFromPortal() ? "portal" : "env";
        boolean tokenValid = false;
        String phone = null, name = null, quality = null, codeStatus = null, message;
        Integer templateCount = null;

        try {
            JsonNode info = whatsApp.phoneInfo();
            tokenValid = true;
            phone = text(info, "display_phone_number");
            name = text(info, "verified_name");
            quality = text(info, "quality_rating");
            codeStatus = text(info, "code_verification_status");
            message = "Connected.";
        } catch (Exception e) {
            message = "Token check failed: " + trim(e.getMessage());
            log.warn("WhatsApp connection check failed: {}", e.getMessage());
        }

        if (tokenValid) {
            try {
                templateCount = whatsApp.fetchTemplates().size();
            } catch (Exception e) {
                log.debug("Template count unavailable: {}", e.getMessage());
            }
        }

        return new ConnectionStatusDto(true, tokenValid, source, whatsApp.phoneId(),
                phone, name, quality, codeStatus, whatsApp.wabaId(), templateCount, message);
    }

    @Override
    public ConnectionStatusDto save(ConnectionRequest r) {
        putIfPresent(WhatsAppService.KEY_TOKEN, r.token());
        putIfPresent(WhatsAppService.KEY_PHONE_ID, r.phoneId());
        putIfPresent(WhatsAppService.KEY_WABA_ID, r.wabaId());
        putIfPresent(WhatsAppService.KEY_DEFAULT_CC, r.defaultCc());
        putIfPresent(WhatsAppService.KEY_VERIFY_TOKEN, r.verifyToken());
        putIfPresent(WhatsAppService.KEY_APP_SECRET, r.appSecret());
        whatsApp.reload();
        return status();
    }

    @Override
    public ConnectionStatusDto detectWaba() {
        if (!whatsApp.isConfigured()) {
            throw new BusinessException("Save a valid access token first, then detect the WABA.");
        }
        String waba;
        try {
            waba = whatsApp.detectWabaId();
        } catch (Exception e) {
            throw new BusinessException("WABA detection failed: " + trim(e.getMessage()));
        }
        if (waba == null || waba.isBlank()) {
            throw new BusinessException("Could not detect a WhatsApp Business Account from this token. "
                    + "Enter the WABA ID manually (Meta Business Manager → WhatsApp Accounts).");
        }
        settings.upsert(WhatsAppService.KEY_WABA_ID, waba);
        whatsApp.reload();
        return status();
    }

    private void putIfPresent(String key, String value) {
        if (value != null && !value.isBlank()) {
            settings.upsert(key, value.trim());
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static String trim(String s) {
        if (s == null) return "unknown error";
        return s.length() <= 300 ? s : s.substring(0, 300);
    }
}
