package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.dto.order.InvoiceDto;
import in.jlenterprises.ecommerce.dto.order.OrderDto;
import in.jlenterprises.ecommerce.dto.order.OrderSummaryDto;
import in.jlenterprises.ecommerce.dto.order.OrderTrackingDto;
import in.jlenterprises.ecommerce.request.order.PlaceOrderRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    // ── Customer ──
    OrderDto placeOrder(UUID userId, PlaceOrderRequest request);

    Page<OrderSummaryDto> myOrders(UUID userId, Pageable pageable);

    OrderDto myOrder(UUID userId, UUID orderId);

    OrderDto cancel(UUID userId, UUID orderId);

    InvoiceDto invoice(UUID userId, UUID orderId);

    /** Public tracking: returns status only when the phone matches the order's shipping phone. */
    OrderTrackingDto track(String orderNumber, String phone);

    // ── Admin ──
    Page<OrderSummaryDto> listAll(OrderStatus status, Pageable pageable);

    Page<OrderSummaryDto> userOrders(UUID userId, Pageable pageable);

    OrderDto adminGet(UUID orderId);

    OrderDto updateStatus(UUID orderId, OrderStatus status);
}
