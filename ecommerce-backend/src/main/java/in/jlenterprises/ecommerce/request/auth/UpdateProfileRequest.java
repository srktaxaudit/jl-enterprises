package in.jlenterprises.ecommerce.request.auth;

import in.jlenterprises.ecommerce.validation.ValidationPatterns;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 80) @Pattern(regexp = ValidationPatterns.NAME_OPT, message = ValidationPatterns.MSG_NAME) String firstName,
        @Size(max = 80) @Pattern(regexp = ValidationPatterns.NAME_OPT, message = ValidationPatterns.MSG_NAME) String lastName,
        @Pattern(regexp = ValidationPatterns.PHONE_OPT, message = ValidationPatterns.MSG_PHONE) String phone,
        Boolean whatsappOptIn
) {}
