package in.jlenterprises.ecommerce.dto.order;

public record AddressSnapshotDto(
        String fullName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country
) {}
