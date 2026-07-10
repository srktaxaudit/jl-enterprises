package in.jlenterprises.ecommerce.dto.whatsapp;

import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;

import java.time.Instant;
import java.util.UUID;

/** One row in the global delivery log (across all campaigns). */
public record DeliveryLogDto(
        UUID id,
        UUID campaignId,
        String campaignName,
        String recipientName,
        String phone,
        WhatsappMessageStatus messageStatus,
        String providerMessageId,
        String error,
        int attempts,
        Instant createdAt,
        Instant sentAt,
        Instant deliveredAt,
        Instant readAt
) {}
