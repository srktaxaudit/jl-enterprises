package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.WhatsappAudienceType;
import in.jlenterprises.ecommerce.constant.WhatsappAutomationEvent;
import in.jlenterprises.ecommerce.constant.WhatsappCampaignStatus;
import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;
import in.jlenterprises.ecommerce.dto.whatsapp.AutomationRuleDto;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.entity.WhatsappAutomationRule;
import in.jlenterprises.ecommerce.entity.WhatsappCampaign;
import in.jlenterprises.ecommerce.entity.WhatsappMessageLog;
import in.jlenterprises.ecommerce.entity.WhatsappTemplate;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.notification.WhatsAppService;
import in.jlenterprises.ecommerce.repository.WhatsappAutomationRuleRepository;
import in.jlenterprises.ecommerce.repository.WhatsappCampaignRepository;
import in.jlenterprises.ecommerce.repository.WhatsappMessageLogRepository;
import in.jlenterprises.ecommerce.repository.WhatsappTemplateRepository;
import in.jlenterprises.ecommerce.request.whatsapp.AutomationRuleRequest;
import in.jlenterprises.ecommerce.service.WhatsappAutomationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WhatsappAutomationServiceImpl implements WhatsappAutomationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappAutomationServiceImpl.class);

    private final WhatsappAutomationRuleRepository ruleRepo;
    private final WhatsappTemplateRepository templateRepo;
    private final WhatsappCampaignRepository campaignRepo;
    private final WhatsappMessageLogRepository logRepo;
    private final WhatsAppService whatsApp;

    public WhatsappAutomationServiceImpl(WhatsappAutomationRuleRepository ruleRepo,
                                         WhatsappTemplateRepository templateRepo,
                                         WhatsappCampaignRepository campaignRepo,
                                         WhatsappMessageLogRepository logRepo,
                                         WhatsAppService whatsApp) {
        this.ruleRepo = ruleRepo;
        this.templateRepo = templateRepo;
        this.campaignRepo = campaignRepo;
        this.logRepo = logRepo;
        this.whatsApp = whatsApp;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationRuleDto> list() {
        Map<WhatsappAutomationEvent, WhatsappAutomationRule> byEvent = ruleRepo.findAll().stream()
                .collect(Collectors.toMap(WhatsappAutomationRule::getEvent, r -> r, (a, b) -> a));
        List<AutomationRuleDto> out = new ArrayList<>();
        for (WhatsappAutomationEvent event : WhatsappAutomationEvent.values()) {
            out.add(toDto(event, byEvent.get(event)));
        }
        return out;
    }

    @Override
    @Transactional
    public AutomationRuleDto update(WhatsappAutomationEvent event, AutomationRuleRequest r) {
        WhatsappAutomationRule rule = ruleRepo.findByEvent(event).orElseGet(() -> {
            WhatsappAutomationRule n = new WhatsappAutomationRule();
            n.setEvent(event);
            return n;
        });
        if (r.templateId() != null) {
            WhatsappTemplate t = templateRepo.findById(r.templateId())
                    .orElseThrow(() -> new BusinessException("That template no longer exists."));
            rule.setTemplate(t);
        } else {
            rule.setTemplate(null);
        }
        if (r.enabled() && rule.getTemplate() == null) {
            throw new BusinessException("Pick a template before enabling this automation.");
        }
        rule.setEnabled(r.enabled());
        return toDto(event, ruleRepo.save(rule));
    }

    @Override
    @Transactional
    public void fire(WhatsappAutomationEvent event, Order order) {
        try {
            WhatsappAutomationRule rule = ruleRepo.findByEvent(event).orElse(null);
            if (rule == null || !rule.isEnabled() || rule.getTemplate() == null || order == null) return;
            User u = order.getUser();
            if (u == null || u.getPhone() == null || u.getPhone().isBlank()) return;

            WhatsappTemplate t = rule.getTemplate();
            boolean demo = !whatsApp.isConfigured();
            // Live automation needs a Meta-registered template (business-initiated messages
            // outside the 24h window must be templated). Demo mode simulates + logs.
            if (!demo && (t.getMetaTemplateName() == null || t.getMetaTemplateName().isBlank())) {
                log.warn("WhatsApp automation {} skipped: template '{}' has no Meta template name", event, t.getName());
                return;
            }

            String firstName = firstNameOf(u);
            String orderNo = order.getOrderNumber() == null ? "" : order.getOrderNumber();
            String amount = order.getGrandTotal() == null ? "" : "₹" + order.getGrandTotal().toPlainString();

            WhatsappCampaign c = autoCampaign(event, t, demo);
            WhatsappMessageLog msg = new WhatsappMessageLog();
            msg.setCampaign(c);
            msg.setUserId(u.getId());
            msg.setRecipientName(nameOf(u));
            msg.setPhone(u.getPhone());
            msg.setRenderedBody(render(t.getBodyText(), firstName, orderNo, amount));
            msg.setAttempts(1);
            try {
                String providerId = demo
                        ? "demo-" + UUID.randomUUID()
                        : whatsApp.sendTemplate(u.getPhone(), t.getMetaTemplateName(), t.getLanguage(),
                                List.of(firstName, orderNo, amount));
                msg.setProviderMessageId(providerId);
                msg.setMessageStatus(WhatsappMessageStatus.SENT);
                msg.setSentAt(Instant.now());
                c.setSentCount(c.getSentCount() + 1);
            } catch (Exception e) {
                msg.setMessageStatus(WhatsappMessageStatus.FAILED);
                msg.setError(trim(e.getMessage()));
                c.setFailedCount(c.getFailedCount() + 1);
                log.warn("WhatsApp automation {} send failed for {}: {}", event, u.getPhone(), e.getMessage());
            }
            c.setTotalRecipients(c.getTotalRecipients() + 1);
            c.setSentAt(Instant.now());
            campaignRepo.save(c);
            logRepo.save(msg);
        } catch (Exception e) {
            // Automation must never break the business flow that triggered it.
            log.warn("WhatsApp automation {} failed: {}", event, e.getMessage());
        }
    }

    /** Find or create the container campaign that collects this event's sends. */
    private WhatsappCampaign autoCampaign(WhatsappAutomationEvent event, WhatsappTemplate t, boolean demo) {
        return campaignRepo.findFirstByAutomationEvent(event.name()).orElseGet(() -> {
            WhatsappCampaign c = new WhatsappCampaign();
            c.setName("⚙ Auto: " + label(event));
            c.setAutomationEvent(event.name());
            c.setAudienceType(WhatsappAudienceType.MANUAL);
            c.setBodyText(t.getBodyText());
            c.setTemplate(t);
            c.setCampaignStatus(WhatsappCampaignStatus.COMPLETED);
            c.setDemoMode(demo);
            return campaignRepo.save(c);
        });
    }

    private AutomationRuleDto toDto(WhatsappAutomationEvent event, WhatsappAutomationRule rule) {
        WhatsappTemplate t = rule == null ? null : rule.getTemplate();
        int sent = 0, failed = 0;
        WhatsappCampaign c = campaignRepo.findFirstByAutomationEvent(event.name()).orElse(null);
        if (c != null) { sent = c.getSentCount(); failed = c.getFailedCount(); }
        return new AutomationRuleDto(event, rule != null && rule.isEnabled(),
                t == null ? null : t.getId(), t == null ? null : t.getName(),
                t == null ? null : t.getMetaStatus(), sent, failed);
    }

    private static String label(WhatsappAutomationEvent e) {
        return switch (e) {
            case ORDER_PLACED -> "Order placed";
            case PAYMENT_RECEIVED -> "Payment received";
            case ORDER_SHIPPED -> "Order shipped";
            case ORDER_OUT_FOR_DELIVERY -> "Out for delivery";
            case ORDER_DELIVERED -> "Order delivered";
            case ORDER_CANCELLED -> "Order cancelled";
            case ABANDONED_CHECKOUT -> "Abandoned checkout";
        };
    }

    private static String render(String body, String name, String orderNo, String amount) {
        String s = body == null ? "" : body;
        s = s.replace("{{name}}", name).replace("{name}", name)
             .replace("{{order}}", orderNo).replace("{order}", orderNo)
             .replace("{{amount}}", amount).replace("{amount}", amount);
        return s.length() > 2000 ? s.substring(0, 2000) : s;
    }

    private static String nameOf(User u) {
        String n = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return n.isEmpty() ? u.getEmail() : n;
    }

    private static String firstNameOf(User u) {
        String f = u.getFirstName();
        return (f == null || f.isBlank()) ? "there" : f.trim().split("\\s+")[0];
    }

    private static String trim(String s) {
        if (s == null) return null;
        return s.length() <= 500 ? s : s.substring(0, 500);
    }
}
