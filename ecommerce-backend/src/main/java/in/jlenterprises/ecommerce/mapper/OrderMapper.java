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
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hand-written order mapper — nested snapshots, the lazy payment association and
 * the derived coupon code / item count are clearer mapped explicitly than via
 * MapStruct. Must be called inside an open transaction (touches lazy fields).
 */
@Component
public class OrderMapper {

    private static final String SELLER = "JL Enterprises";
    private static final String SELLER_ADDRESS = "185G/1B, Palai Road, Chidambaramnagar, Thoothukudi, Tamil Nadu 628008";

    public OrderDto toDto(Order o) {
        return new OrderDto(
                o.getId(), o.getOrderNumber(), o.getOrderStatus(),
                o.getSubtotal(), o.getDiscountTotal(), o.getTaxTotal(), o.getShippingTotal(), o.getGrandTotal(),
                o.getCurrency(),
                o.getCoupon() == null ? null : o.getCoupon().getCode(),
                toSnapshotDto(o.getShippingAddress()),
                toSnapshotDto(o.getBillingAddress()),
                o.getNotes(), o.getPlacedAt(),
                o.getItems().stream().map(this::toItemDto).toList(),
                toPaymentDto(o.getPayment())
        );
    }

    public OrderSummaryDto toSummary(Order o) {
        return new OrderSummaryDto(
                o.getId(), o.getOrderNumber(), o.getOrderStatus(),
                o.getGrandTotal(), o.getCurrency(), o.getItems().size(), o.getPlacedAt());
    }

    public InvoiceDto toInvoice(Order o) {
        List<OrderItemDto> items = o.getItems().stream().map(this::toItemDto).toList();
        return new InvoiceDto(
                "INV-" + o.getOrderNumber(), o.getOrderNumber(), o.getPlacedAt(),
                SELLER, SELLER_ADDRESS,
                toSnapshotDto(o.getBillingAddress()),
                items,
                o.getSubtotal(), o.getDiscountTotal(), o.getTaxTotal(), o.getShippingTotal(), o.getGrandTotal(),
                o.getCurrency());
    }

    private OrderItemDto toItemDto(OrderItem i) {
        return new OrderItemDto(
                i.getId(),
                i.getProduct() == null ? null : i.getProduct().getId(),
                i.getVariant() == null ? null : i.getVariant().getId(),
                i.getProductName(), i.getSku(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal());
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
