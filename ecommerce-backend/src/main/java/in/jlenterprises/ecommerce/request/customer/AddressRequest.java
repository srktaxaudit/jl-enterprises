package in.jlenterprises.ecommerce.request.customer;

import in.jlenterprises.ecommerce.constant.AddressType;
import in.jlenterprises.ecommerce.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        AddressType type,
        @Size(max = 120) @Pattern(regexp = ValidationPatterns.NAME_OPT, message = ValidationPatterns.MSG_NAME) String fullName,
        @Size(max = 20) @Pattern(regexp = ValidationPatterns.PHONE_OPT, message = ValidationPatterns.MSG_PHONE) String phone,
        @NotBlank @Size(max = 200) String line1,
        @Size(max = 200) String line2,
        @NotBlank @Size(max = 80) @Pattern(regexp = ValidationPatterns.NAME, message = ValidationPatterns.MSG_NAME) String city,
        @Size(max = 80) @Pattern(regexp = ValidationPatterns.NAME_OPT, message = ValidationPatterns.MSG_NAME) String state,
        @NotBlank @Size(max = 20) @Pattern(regexp = ValidationPatterns.POSTAL, message = ValidationPatterns.MSG_POSTAL) String postalCode,
        @Size(max = 80) String country,
        boolean defaultAddress
) {}
