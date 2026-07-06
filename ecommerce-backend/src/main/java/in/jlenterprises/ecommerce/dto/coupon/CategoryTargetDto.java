package in.jlenterprises.ecommerce.dto.coupon;

import java.util.UUID;

/** Compact category reference exposed with a coupon. */
public record CategoryTargetDto(UUID id, String name, String slug) {}
