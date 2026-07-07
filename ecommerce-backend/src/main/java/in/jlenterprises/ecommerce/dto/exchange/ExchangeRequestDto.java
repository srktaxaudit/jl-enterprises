package in.jlenterprises.ecommerce.dto.exchange;

import in.jlenterprises.ecommerce.constant.ExchangeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Exchange request as returned to customers (their own) and staff. */
public record ExchangeRequestDto(
        UUID id,
        String customerName,
        String customerEmail,
        String applianceCategory,
        String brand,
        String modelNumber,
        Integer purchaseYear,
        String conditionGrade,
        boolean working,
        String reason,
        List<String> imageUrls,
        BigDecimal expectedValue,
        BigDecimal estimatedValue,
        BigDecimal finalValue,
        UUID desiredProductId,
        String desiredProductName,
        ExchangeStatus status,
        String internalNotes,
        UUID appliedOrderId,
        Instant createdAt,
        Instant updatedAt
) {}
