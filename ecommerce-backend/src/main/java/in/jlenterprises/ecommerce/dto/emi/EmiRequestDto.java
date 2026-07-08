package in.jlenterprises.ecommerce.dto.emi;

import java.time.Instant;
import java.util.UUID;

public record EmiRequestDto(
        UUID id,
        String customerName,
        String phone,
        UUID productId,
        String productName,
        Integer months,
        String note,
        String emiStatus,
        Instant createdAt
) {}
