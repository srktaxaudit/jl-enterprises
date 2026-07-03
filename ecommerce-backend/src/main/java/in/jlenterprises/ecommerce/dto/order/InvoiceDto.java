package in.jlenterprises.ecommerce.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** A simple invoice view assembled from an order (PDF rendering is a later concern). */
public record InvoiceDto(
        String invoiceNumber,
        String orderNumber,
        Instant issuedAt,
        String sellerName,
        String sellerAddress,
        AddressSnapshotDto billTo,
        List<OrderItemDto> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxTotal,
        BigDecimal shippingTotal,
        BigDecimal grandTotal,
        String currency
) {}
