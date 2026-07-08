package in.jlenterprises.ecommerce.dto.contact;

import java.time.Instant;
import java.util.UUID;

public record ContactEnquiryDto(
        UUID id,
        String name,
        String email,
        String phone,
        String subject,
        String message,
        String enquiryStatus,
        Instant createdAt
) {}
