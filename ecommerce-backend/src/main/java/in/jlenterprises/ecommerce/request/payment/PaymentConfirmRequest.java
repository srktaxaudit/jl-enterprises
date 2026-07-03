package in.jlenterprises.ecommerce.request.payment;

import jakarta.validation.constraints.NotBlank;

public record PaymentConfirmRequest(
        @NotBlank String providerReference,
        String signature,
        String payload
) {}
