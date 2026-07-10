package in.jlenterprises.ecommerce.dto.whatsapp;

import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;

import java.time.Instant;
import java.util.UUID;

/** One message in an Inbox thread. */
public record ChatMessageDto(
        UUID id,
        String direction,        // IN | OUT
        String messageType,
        String body,
        WhatsappMessageStatus messageStatus,
        String error,
        Instant eventAt
) {}
