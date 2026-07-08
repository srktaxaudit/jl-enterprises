package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.dto.order.InvoiceDto;
import in.jlenterprises.ecommerce.dto.order.OrderDto;
import in.jlenterprises.ecommerce.dto.order.OrderSummaryDto;
import in.jlenterprises.ecommerce.dto.order.OrderTrackingDto;
import in.jlenterprises.ecommerce.request.order.PlaceOrderRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface OrderService {

    // ── Customer ──
    OrderDto placeOrder(UUID userId, PlaceOrderRequest request);

    Page<OrderSummaryDto> myOrders(UUID userId, Pageable pageable);

    OrderDto myOrder(UUID userId, UUID orderId);

    /** Customer cancels their order (allowed only in PENDING/CONFIRMED/PROCESSING). */
    OrderDto cancel(UUID userId, UUID orderId, String reason);

    /** Customer requests a return for a DELIVERED order. */
    OrderDto requestReturn(UUID userId, UUID orderId, String reason);

    InvoiceDto invoice(UUID userId, UUID orderId);

    /** Public tracking: returns status only when the phone matches the order's shipping phone. */
    OrderTrackingDto track(String orderNumber, String phone);

    // ── Admin ──
    Page<OrderSummaryDto> listAll(OrderStatus status, PaymentStatus paymentStatus, Instant from, Instant to, Pageable pageable);

    Page<OrderSummaryDto> userOrders(UUID userId, Pageable pageable);

    OrderDto adminGet(UUID orderId);

    OrderDto updateStatus(UUID orderId, OrderStatus status);

    /** Admin approves a return request → RETURNED, restock + refund. */
    OrderDto approveReturn(UUID orderId);

    /** Admin rejects a return request → back to DELIVERED, with a reason recorded in admin notes. */
    OrderDto rejectReturn(UUID orderId, String reason);

    /** Admin sets/updates the internal notes on an order. */
    OrderDto setAdminNotes(UUID orderId, String notes);
}
