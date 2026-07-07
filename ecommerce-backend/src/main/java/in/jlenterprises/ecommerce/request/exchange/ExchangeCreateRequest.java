package in.jlenterprises.ecommerce.request.exchange;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Payload for a customer submitting an exchange (trade-in) request. */
public record ExchangeCreateRequest(
        @NotBlank @Size(max = 80) String applianceCategory,
        @Size(max = 80) String brand,
        @Size(max = 120) String modelNumber,
        Integer purchaseYear,
        @Size(max = 20) String conditionGrade,
        Boolean working,
        @Size(max = 500) String reason,
        @PositiveOrZero BigDecimal expectedValue,
        UUID desiredProductId,
        @Size(max = 200) String desiredProductName,
        // URLs already uploaded via POST /api/v1/exchanges/images (optional at create time).
        List<String> imageUrls
) {}
