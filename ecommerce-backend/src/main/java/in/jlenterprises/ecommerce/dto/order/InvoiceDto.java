package in.jlenterprises.ecommerce.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * An invoice view assembled from an order. Prices are GST-inclusive (Indian retail),
 * so the GST fields (taxableValue / cgst / sgst / igst / taxTotal) are the tax embedded
 * in the grand total — the amount charged is unchanged. Tax is computed PER LINE at each
 * item's snapshotted rate; {@code gstRate} remains as the store default for legacy
 * consumers. Intra-state supplies split CGST/SGST (igst = 0); inter-state supplies use
 * IGST (cgst = sgst = 0), decided by ship-to state vs the seller's state.
 */
public record InvoiceDto(
        String invoiceNumber,
        String orderNumber,
        Instant issuedAt,
        String sellerName,
        String sellerAddress,
        String sellerGstin,
        AddressSnapshotDto billTo,
        List<OrderItemDto> items,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxableValue,
        BigDecimal gstRate,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst,
        boolean interState,
        BigDecimal taxTotal,
        BigDecimal shippingTotal,
        BigDecimal grandTotal,
        String currency,
        String paymentMethod,
        String paymentStatus
) {}
