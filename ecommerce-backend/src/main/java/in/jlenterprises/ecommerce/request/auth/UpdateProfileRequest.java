package in.jlenterprises.ecommerce.request.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Pattern(regexp = "^$|^[0-9+][0-9]{7,14}$", message = "Invalid phone number") String phone
) {}
