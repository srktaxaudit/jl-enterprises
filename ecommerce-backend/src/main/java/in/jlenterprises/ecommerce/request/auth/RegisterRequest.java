package in.jlenterprises.ecommerce.request.auth;

import in.jlenterprises.ecommerce.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @StrongPassword String password,
        @NotBlank @Size(max = 80) String firstName,
        @Size(max = 80) String lastName,
        @Pattern(regexp = "^$|^[0-9+][0-9]{7,14}$", message = "Invalid phone number") String phone
) {}
