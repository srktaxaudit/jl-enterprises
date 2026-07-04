package in.jlenterprises.ecommerce.request.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Public service-booking submission from the storefront. */
public record ServiceBookingRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "^[0-9+][0-9]{7,14}$", message = "Invalid phone number") String phone,
        @Size(max = 80) String serviceType,
        @Size(max = 1000) String message,
        @Size(max = 60) String preferredDate
) {}
