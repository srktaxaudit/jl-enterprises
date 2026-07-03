package in.jlenterprises.ecommerce.request.auth;

import in.jlenterprises.ecommerce.constant.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyOtpRequest(
        @NotBlank String identifier,
        @NotNull OtpPurpose purpose,
        @NotBlank String code
) {}
