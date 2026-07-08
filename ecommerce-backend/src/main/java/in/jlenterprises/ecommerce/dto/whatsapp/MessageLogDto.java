package in.jlenterprises.ecommerce.dto.whatsapp;

import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;

import java.time.Instant;
import java.util.UUID;

public record MessageLogDto(
        UUID id,
        UUID userId,
        String recipientName,
        String phone,
        String renderedBody,
        WhatsappMessageStatus messageStatus,
        String providerMessageId,
        String error,
        int attempts,
        Instant sentAt
) {}
