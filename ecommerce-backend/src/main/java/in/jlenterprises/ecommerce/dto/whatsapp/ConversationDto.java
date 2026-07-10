package in.jlenterprises.ecommerce.dto.whatsapp;

import java.time.Instant;
import java.util.UUID;

/** One Inbox conversation row. */
public record ConversationDto(
        UUID id,
        String phone,
        String contactName,
        UUID userId,
        String customerName,     // store account name when the phone matched a user
        Instant lastMessageAt,
        Instant lastInboundAt,
        String lastPreview,
        int unreadCount,
        boolean windowOpen       // free-text replies allowed (customer wrote within 24h)
) {}
