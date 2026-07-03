package in.jlenterprises.ecommerce.request.order;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Checkout payload — the order is built from the caller's current cart. */
public record PlaceOrderRequest(
        @NotNull UUID shippingAddressId,
        UUID billingAddressId,
        String couponCode,
        @NotNull PaymentMethod paymentMethod,
        @Size(max = 500) String notes
) {}
