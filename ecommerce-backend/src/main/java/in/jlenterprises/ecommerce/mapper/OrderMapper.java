package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.order.AddressSnapshotDto;
import in.jlenterprises.ecommerce.dto.order.InvoiceDto;
import in.jlenterprises.ecommerce.dto.order.OrderDto;
import in.jlenterprises.ecommerce.dto.order.OrderItemDto;
import in.jlenterprises.ecommerce.dto.order.OrderPaymentDto;
import in.jlenterprises.ecommerce.dto.order.OrderSummaryDto;
import in.jlenterprises.ecommerce.entity.AddressSnapshot;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.OrderItem;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.util.GstUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Hand-written order mapper — nested snapshots, the lazy payment association and
 * the derived coupon code / item count are clearer mapped explicitly than via
 * MapStruct. Must be called inside an open transaction (touches lazy fields).
 */
@Component
public class OrderMapper {

    public OrderDto toDto(Order o) {
        return new OrderDto(
                o.getId(), o.getOrderNumber(), o.getOrderStatus(),
                o.getSubtotal(), o.getDiscountTotal(), o.getTaxTotal(), o.getShippingTotal(), o.getGrandTotal(),
                o.getCurrency(),
                o.getCoupon() == null ? null : o.getCoupon().getCode(),
                toSnapshotDto(o.getShippingAddress()),
                toSnapshotDto(o.getBillingAddress()),
                o.getNotes(), o.getPlacedAt(),
                o.getCancelledAt(), o.getCancellationReason(), o.getReturnReason(), o.getAdminNotes(),
                o.getItems().stream().map(this::toItemDto).toList(),
                toPaymentDto(o.getPayment())
        );
    }

    public OrderSummaryDto toSummary(Order o) {
        Payment p = o.getPayment();
        return new OrderSummaryDto(
                o.getId(), o.getOrderNumber(), o.getOrderStatus(),
                o.getGrandTotal(), o.getCurrency(), o.getItems().size(), o.getPlacedAt(),
                p == null ? null : p.getMethod(),
                p == null ? null : p.getPaymentStatus());
    }

    public InvoiceDto toInvoice(Order o, BigDecimal gstRate, String sellerGstin,
                                String sellerName, String sellerAddress, String sellerState) {
        List<OrderItemDto> items = o.getItems().stream().map(this::toItemDto).toList();
        BigDecimal grand = o.getGrandTotal() == null ? BigDecimal.ZERO : o.getGrandTotal();
        // Prices are GST-inclusive: derive the embedded tax (total unchanged), computed
        // per line at each item's snapshotted rate — same math as the journal posting.
        BigDecimal gst = GstUtil.embeddedGst(o, grand, gstRate);
        BigDecimal taxable = grand.subtract(gst);
        // Place of supply for goods = where they ship. Same state as the seller →
        // CGST+SGST split; different state → the whole tax is IGST.
        boolean interState = isInterState(o, sellerState);
        BigDecimal cgst, sgst, igst;
        if (interState) {
            igst = gst; cgst = BigDecimal.ZERO; sgst = BigDecimal.ZERO;
        } else {
            cgst = gst.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            sgst = gst.subtract(cgst);   // remainder absorbs any rounding
            igst = BigDecimal.ZERO;
        }
        Payment p = o.getPayment();
        return new InvoiceDto(
                "INV-" + o.getOrderNumber(), o.getOrderNumber(), o.getPlacedAt(),
                sellerName, sellerAddress, sellerGstin == null ? "" : sellerGstin,
                toSnapshotDto(o.getBillingAddress()),
                items,
                o.getSubtotal(), o.getDiscountTotal(),
                taxable, gstRate, cgst, sgst, igst, interState, gst,
                o.getShippingTotal(), grand, o.getCurrency(),
                p == null ? null : p.getMethod().name(),
                p == null ? null : p.getPaymentStatus().name());
    }

    private static boolean isInterState(Order o, String sellerState) {
        if (sellerState == null || sellerState.isBlank()) return false;
        AddressSnapshot ship = o.getShippingAddress() != null ? o.getShippingAddress() : o.getBillingAddress();
        String state = ship == null ? null : ship.getState();
        // Unknown ship-to state → assume intra-state (the store's walk-in default).
        if (state == null || state.isBlank()) return false;
        return !state.trim().equalsIgnoreCase(sellerState.trim());
    }

    private OrderItemDto toItemDto(OrderItem i) {
        return new OrderItemDto(
                i.getId(),
                i.getProduct() == null ? null : i.getProduct().getId(),
                i.getVariant() == null ? null : i.getVariant().getId(),
                i.getProductName(), i.getSku(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal(),
                i.getGstRate(), i.getHsnCode());
    }

    private AddressSnapshotDto toSnapshotDto(AddressSnapshot a) {
        if (a == null) return null;
        return new AddressSnapshotDto(a.getFullName(), a.getPhone(), a.getLine1(), a.getLine2(),
                a.getCity(), a.getState(), a.getPostalCode(), a.getCountry());
    }

    private OrderPaymentDto toPaymentDto(Payment p) {
        if (p == null) return null;
        return new OrderPaymentDto(p.getMethod(), p.getPaymentStatus(), p.getAmount(),
                p.getCurrency(), p.getProviderPaymentId());
    }
}
