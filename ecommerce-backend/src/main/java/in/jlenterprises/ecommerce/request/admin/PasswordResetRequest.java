package in.jlenterprises.ecommerce.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** New password for an admin-initiated staff password reset. Sent in the request
 *  body (never as a query parameter) so it never lands in access logs or history. */
public record PasswordResetRequest(
        @NotBlank @Size(min = 8, max = 100) String password
) {}
