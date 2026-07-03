package in.jlenterprises.ecommerce.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BroadcastRequest(
        @NotBlank @Size(max = 1000) String message,
        boolean onlyVerified
) {}
