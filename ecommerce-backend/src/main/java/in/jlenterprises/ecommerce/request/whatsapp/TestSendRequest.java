package in.jlenterprises.ecommerce.request.whatsapp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Send one test message to a single number to verify setup before broadcasting. */
public record TestSendRequest(
        @NotBlank @Size(max = 20) String phone,
        UUID templateId,
        @Size(max = 2000) String bodyText
) {}
