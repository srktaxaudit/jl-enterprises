package in.jlenterprises.ecommerce.request.service;

import in.jlenterprises.ecommerce.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Public service-booking submission from the storefront. */
public record ServiceBookingRequest(
        @NotBlank @Size(max = 120) @Pattern(regexp = ValidationPatterns.NAME, message = ValidationPatterns.MSG_NAME) String name,
        @NotBlank @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.MSG_PHONE) String phone,
        @Size(max = 80) String serviceType,
        @Size(max = 1000) String message,
        @Size(max = 60) String preferredDate
) {}
