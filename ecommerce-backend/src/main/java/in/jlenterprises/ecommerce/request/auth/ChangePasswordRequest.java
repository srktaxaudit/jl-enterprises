package in.jlenterprises.ecommerce.request.auth;

import in.jlenterprises.ecommerce.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @StrongPassword String newPassword
) {}
