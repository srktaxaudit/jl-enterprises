package in.jlenterprises.ecommerce.dto.customer;

import in.jlenterprises.ecommerce.constant.AddressType;

import java.util.UUID;

public record AddressDto(
        UUID id,
        AddressType type,
        String fullName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country,
        boolean defaultAddress
) {}
