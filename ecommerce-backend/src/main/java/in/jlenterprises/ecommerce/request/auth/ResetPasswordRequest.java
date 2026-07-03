package in.jlenterprises.ecommerce.request.auth;

import in.jlenterprises.ecommerce.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @StrongPassword String newPassword
) {}
