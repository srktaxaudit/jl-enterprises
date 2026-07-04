package in.jlenterprises.ecommerce.dto.service;

import java.time.Instant;
import java.util.UUID;

public record ServiceBookingDto(
        UUID id,
        String customerName,
        String phone,
        String serviceType,
        String message,
        String preferredDate,
        String bookingStatus,
        Instant createdAt
) {}
