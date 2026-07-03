package in.jlenterprises.ecommerce.dto.customer;

import in.jlenterprises.ecommerce.constant.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String link,
        boolean read,
        Instant createdAt
) {}
