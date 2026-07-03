package in.jlenterprises.ecommerce.request.auth;

import in.jlenterprises.ecommerce.constant.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendOtpRequest(
        @NotBlank String identifier,   // email or phone
        @NotNull OtpPurpose purpose
) {}
