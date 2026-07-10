package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.whatsapp.ChatMessageDto;
import in.jlenterprises.ecommerce.dto.whatsapp.ConversationDto;
import in.jlenterprises.ecommerce.request.whatsapp.InboxReplyRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Two-way WhatsApp chat over conversations captured by the webhook (Phase 5 Inbox). */
public interface WhatsappInboxService {

    Page<ConversationDto> conversations(Pageable pageable);

    /** Latest messages of one thread (ascending). Also marks the conversation read. */
    List<ChatMessageDto> messages(UUID conversationId);

    /** Send a staff reply — free text inside the 24h window, template otherwise. */
    ChatMessageDto reply(UUID conversationId, InboxReplyRequest request);

    /** Conversations with unread customer messages (for the tab badge). */
    long unreadConversations();
}
