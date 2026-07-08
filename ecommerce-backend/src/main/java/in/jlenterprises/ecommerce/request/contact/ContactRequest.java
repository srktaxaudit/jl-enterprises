package in.jlenterprises.ecommerce.request.contact;

import in.jlenterprises.ecommerce.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Public "Contact Us" submission from the storefront. */
public record ContactRequest(
        @NotBlank @Size(max = 120) @Pattern(regexp = ValidationPatterns.NAME, message = ValidationPatterns.MSG_NAME) String name,
        @Email @Size(max = 160) String email,
        @Size(max = 20) String phone,
        @Size(max = 160) String subject,
        @NotBlank @Size(max = 2000) String message
) {}
