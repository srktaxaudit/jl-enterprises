package in.jlenterprises.ecommerce.request.emi;

import in.jlenterprises.ecommerce.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Public "Request EMI" submission from a product page. */
public record EmiRequestCreate(
        @NotBlank @Size(max = 120) @Pattern(regexp = ValidationPatterns.NAME, message = ValidationPatterns.MSG_NAME) String name,
        @NotBlank @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.MSG_PHONE) String phone,
        UUID productId,
        @Size(max = 200) String productName,
        @PositiveOrZero Integer months,
        @Size(max = 500) String note
) {}
