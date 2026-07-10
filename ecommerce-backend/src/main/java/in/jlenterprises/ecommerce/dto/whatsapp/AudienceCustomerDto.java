package in.jlenterprises.ecommerce.dto.whatsapp;

import java.util.UUID;

/** One selectable customer in the broadcast audience picker. */
public record AudienceCustomerDto(
        UUID id,
        String name,
        String phone,
        String city,
        boolean optedIn,
        boolean phoneVerified,
        boolean hasOrdered
) {}
