package in.jlenterprises.ecommerce.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One row in the admin billing / invoices ledger. */
public record BillingRowDto(
        UUID orderId,
        String orderNumber,
        String customerName,
        String customerEmail,
        Instant placedAt,
        String orderStatus,
        String paymentMethod,
        String paymentStatus,
        BigDecimal grandTotal,
        BigDecimal taxTotal,
        String currency
) {}
