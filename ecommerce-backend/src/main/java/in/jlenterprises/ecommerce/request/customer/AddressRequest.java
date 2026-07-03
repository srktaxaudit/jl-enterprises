package in.jlenterprises.ecommerce.request.customer;

import in.jlenterprises.ecommerce.constant.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        AddressType type,
        @Size(max = 120) String fullName,
        @Size(max = 20) String phone,
        @NotBlank @Size(max = 200) String line1,
        @Size(max = 200) String line2,
        @NotBlank @Size(max = 80) String city,
        @Size(max = 80) String state,
        @NotBlank @Size(max = 20) String postalCode,
        @Size(max = 80) String country,
        boolean defaultAddress
) {}
