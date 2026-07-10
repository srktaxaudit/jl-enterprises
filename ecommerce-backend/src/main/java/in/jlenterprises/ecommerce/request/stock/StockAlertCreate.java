package in.jlenterprises.ecommerce.request.stock;

import in.jlenterprises.ecommerce.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Public "Notify me when back in stock" submission from a product page. */
public record StockAlertCreate(
        @NotBlank @Size(max = 120) @Pattern(regexp = ValidationPatterns.NAME, message = ValidationPatterns.MSG_NAME) String name,
        @NotBlank @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.MSG_PHONE) String phone,
        UUID productId,
        @Size(max = 200) String productName
) {}
