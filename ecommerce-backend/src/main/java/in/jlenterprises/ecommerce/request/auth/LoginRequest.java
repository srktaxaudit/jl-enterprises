package in.jlenterprises.ecommerce.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login credentials. The {@code email} field is the identifier and accepts EITHER
 * an email address OR a mobile number (resolved server-side), so it is not
 * constrained to email format.
 */
public record LoginRequest(
        @NotBlank @Size(max = 160) String email,
        @NotBlank String password,
        boolean rememberMe
) {}
