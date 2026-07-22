package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.payment.PaymentStrategyFactory;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.repository.PaymentRepository;
import in.jlenterprises.ecommerce.request.payment.PaymentConfirmRequest;
import in.jlenterprises.ecommerce.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Guards the payment-confirm behaviour hardened in the audit follow-up: a customer must not
 * be able to self-confirm a COD order, and a repeated confirm must be idempotent.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock PaymentStrategyFactory strategyFactory;
    @Mock NotificationService notificationService;
    @Mock AccountingService accountingService;

    @InjectMocks PaymentServiceImpl paymentService;

    private final UUID userId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @Test
    void codConfirmIsNoOp_orderStaysPending() {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.PENDING);
        Payment payment = payment(PaymentMethod.COD, PaymentStatus.PENDING, order);
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        paymentService.confirm(userId, orderId, new PaymentConfirmRequest("ref", null, null));

        // COD is settled at delivery — confirm must not mark it paid or move the order.
        assertEquals(OrderStatus.PENDING, order.getOrderStatus());
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
        assertTrue(payment.getTransactions().isEmpty());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(strategyFactory, notificationService, accountingService);
    }

    @Test
    void confirmIsIdempotent_whenAlreadySuccess() {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.CONFIRMED);
        Payment payment = payment(PaymentMethod.RAZORPAY, PaymentStatus.SUCCESS, order);
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        paymentService.confirm(userId, orderId, new PaymentConfirmRequest("ref", null, null));

        // Already settled → no duplicate transaction, no re-fired notifications, no re-post.
        assertTrue(payment.getTransactions().isEmpty());
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(strategyFactory, notificationService, accountingService);
    }

    @Test
    void initiateRejectsAlreadySuccessfulPayment() {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.CONFIRMED);
        Payment payment = payment(PaymentMethod.RAZORPAY, PaymentStatus.SUCCESS, order);
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        // Re-initiating would reset SUCCESS → PENDING and mint a new provider order,
        // making a paid order look unpaid (and payable twice).
        org.junit.jupiter.api.Assertions.assertThrows(
                in.jlenterprises.ecommerce.exception.BusinessException.class,
                () -> paymentService.initiate(userId, orderId));
        assertEquals(PaymentStatus.SUCCESS, payment.getPaymentStatus());
        verifyNoInteractions(strategyFactory);
    }

    @Test
    void initiateRejectsCancelledOrder() {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.CANCELLED);
        Payment payment = payment(PaymentMethod.RAZORPAY, PaymentStatus.PENDING, order);
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        org.junit.jupiter.api.Assertions.assertThrows(
                in.jlenterprises.ecommerce.exception.BusinessException.class,
                () -> paymentService.initiate(userId, orderId));
        verifyNoInteractions(strategyFactory);
    }

    @Test
    void confirmRejectsCancelledOrder_neverMarksPaidOrPostsSale() {
        // The sweeper cancelled (and restocked) this order; a late confirm callback must not
        // flip it to paid and book a sale for stock that was already put back.
        Order order = new Order();
        order.setOrderStatus(OrderStatus.CANCELLED);
        Payment payment = payment(PaymentMethod.RAZORPAY, PaymentStatus.PENDING, order);
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        org.junit.jupiter.api.Assertions.assertThrows(
                in.jlenterprises.ecommerce.exception.BusinessException.class,
                () -> paymentService.confirm(userId, orderId, new PaymentConfirmRequest("ref", "sig", null)));
        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
        assertTrue(payment.getTransactions().isEmpty());
        verifyNoInteractions(strategyFactory, notificationService, accountingService);
    }

    private Payment payment(PaymentMethod method, PaymentStatus status, Order order) {
        Payment p = new Payment();
        p.setMethod(method);
        p.setPaymentStatus(status);
        p.setAmount(new BigDecimal("1000.00"));
        p.setCurrency("INR");
        p.setOrder(order);
        return p;
    }
}
