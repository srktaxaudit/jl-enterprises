package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;
import in.jlenterprises.ecommerce.dto.whatsapp.ChatMessageDto;
import in.jlenterprises.ecommerce.dto.whatsapp.ConversationDto;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.entity.WhatsappChatMessage;
import in.jlenterprises.ecommerce.entity.WhatsappConversation;
import in.jlenterprises.ecommerce.entity.WhatsappTemplate;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.notification.WhatsAppService;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.repository.WhatsappChatMessageRepository;
import in.jlenterprises.ecommerce.repository.WhatsappConversationRepository;
import in.jlenterprises.ecommerce.repository.WhatsappTemplateRepository;
import in.jlenterprises.ecommerce.request.whatsapp.InboxReplyRequest;
import in.jlenterprises.ecommerce.service.WhatsappInboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class WhatsappInboxServiceImpl implements WhatsappInboxService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappInboxServiceImpl.class);
    private static final Duration REPLY_WINDOW = Duration.ofHours(24);

    private final WhatsappConversationRepository conversationRepo;
    private final WhatsappChatMessageRepository chatRepo;
    private final WhatsappTemplateRepository templateRepo;
    private final UserRepository userRepo;
    private final WhatsAppService whatsApp;

    public WhatsappInboxServiceImpl(WhatsappConversationRepository conversationRepo,
                                    WhatsappChatMessageRepository chatRepo,
                                    WhatsappTemplateRepository templateRepo,
                                    UserRepository userRepo,
                                    WhatsAppService whatsApp) {
        this.conversationRepo = conversationRepo;
        this.chatRepo = chatRepo;
        this.templateRepo = templateRepo;
        this.userRepo = userRepo;
        this.whatsApp = whatsApp;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationDto> conversations(Pageable pageable) {
        return conversationRepo.findAllByOrderByLastMessageAtDesc(pageable).map(this::toDto);
    }

    @Override
    @Transactional
    public List<ChatMessageDto> messages(UUID conversationId) {
        WhatsappConversation conv = entity(conversationId);
        if (conv.getUnreadCount() != 0) {
            conv.setUnreadCount(0);
            conversationRepo.save(conv);
        }
        List<WhatsappChatMessage> latest = chatRepo.findTop200ByConversation_IdOrderByEventAtDesc(conversationId);
        List<ChatMessageDto> out = new ArrayList<>(latest.size());
        for (int i = latest.size() - 1; i >= 0; i--) out.add(toDto(latest.get(i)));
        return out;
    }

    @Override
    @Transactional
    public ChatMessageDto reply(UUID conversationId, InboxReplyRequest r) {
        WhatsappConversation conv = entity(conversationId);
        boolean windowOpen = isWindowOpen(conv);
        boolean demo = !whatsApp.isConfigured();

        WhatsappTemplate template = null;
        String body;
        if (r.templateId() != null) {
            template = templateRepo.findById(r.templateId())
                    .orElseThrow(() -> new BusinessException("That template no longer exists."));
            body = template.getBodyText();
        } else if (r.text() != null && !r.text().isBlank()) {
            if (!windowOpen && !demo) {
                throw new BusinessException("The 24-hour reply window has closed — the customer hasn't messaged "
                        + "recently, so WhatsApp only allows an approved template now. Pick a template instead.");
            }
            body = r.text().trim();
        } else {
            throw new BusinessException("Type a message or pick a template.");
        }
        if (!demo && template != null
                && (template.getMetaTemplateName() == null || template.getMetaTemplateName().isBlank())) {
            throw new BusinessException("That template isn't registered in Meta yet — sync templates or pick an approved one.");
        }

        String firstName = firstNameFor(conv);
        String rendered = body.replace("{{name}}", firstName).replace("{name}", firstName);
        if (rendered.length() > 2000) rendered = rendered.substring(0, 2000);

        WhatsappChatMessage msg = new WhatsappChatMessage();
        msg.setConversation(conv);
        msg.setDirection("OUT");
        msg.setMessageType("text");
        msg.setBody(rendered);
        msg.setEventAt(Instant.now());
        try {
            String providerId;
            if (demo) {
                providerId = "demo-" + UUID.randomUUID();
            } else if (template != null) {
                providerId = whatsApp.sendTemplate(conv.getPhone(), template.getMetaTemplateName(),
                        template.getLanguage(), List.of(firstName));
            } else {
                providerId = whatsApp.sendText(conv.getPhone(), rendered);
            }
            msg.setProviderMessageId(providerId);
            msg.setMessageStatus(WhatsappMessageStatus.SENT);
        } catch (Exception e) {
            msg.setMessageStatus(WhatsappMessageStatus.FAILED);
            msg.setError(trim(e.getMessage()));
            log.warn("Inbox reply failed for {}: {}", conv.getPhone(), e.getMessage());
        }
        chatRepo.save(msg);

        conv.setLastMessageAt(msg.getEventAt());
        conv.setLastPreview(rendered.length() > 200 ? rendered.substring(0, 200) : rendered);
        conversationRepo.save(conv);
        return toDto(msg);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadConversations() {
        return conversationRepo.countByUnreadCountGreaterThan(0);
    }

    // ── helpers ──
    private WhatsappConversation entity(UUID id) {
        return conversationRepo.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("WhatsappConversation", id));
    }

    private static boolean isWindowOpen(WhatsappConversation conv) {
        return conv.getLastInboundAt() != null
                && Duration.between(conv.getLastInboundAt(), Instant.now()).compareTo(REPLY_WINDOW) < 0;
    }

    private String firstNameFor(WhatsappConversation conv) {
        if (conv.getUserId() != null) {
            User u = userRepo.findById(conv.getUserId()).orElse(null);
            if (u != null && u.getFirstName() != null && !u.getFirstName().isBlank()) {
                return u.getFirstName().trim().split("\\s+")[0];
            }
        }
        if (conv.getContactName() != null && !conv.getContactName().isBlank()) {
            return conv.getContactName().trim().split("\\s+")[0];
        }
        return "there";
    }

    private ConversationDto toDto(WhatsappConversation c) {
        String customerName = null;
        if (c.getUserId() != null) {
            User u = userRepo.findById(c.getUserId()).orElse(null);
            if (u != null) {
                String n = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                        + (u.getLastName() == null ? "" : u.getLastName())).trim();
                customerName = n.isEmpty() ? u.getEmail() : n;
            }
        }
        return new ConversationDto(c.getId(), c.getPhone(), c.getContactName(), c.getUserId(), customerName,
                c.getLastMessageAt(), c.getLastInboundAt(), c.getLastPreview(), c.getUnreadCount(), isWindowOpen(c));
    }

    private ChatMessageDto toDto(WhatsappChatMessage m) {
        return new ChatMessageDto(m.getId(), m.getDirection(), m.getMessageType(), m.getBody(),
                m.getMessageStatus(), m.getError(), m.getEventAt());
    }

    private static String trim(String s) {
        if (s == null) return null;
        return s.length() <= 500 ? s : s.substring(0, 500);
    }
}
