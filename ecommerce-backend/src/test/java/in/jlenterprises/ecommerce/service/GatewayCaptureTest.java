package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.constant.TransactionType;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.payment.PaymentStrategy;
import in.jlenterprises.ecommerce.payment.PaymentStrategyFactory;
import in.jlenterprises.ecommerce.payment.impl.RazorpayPaymentStrategy;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.repository.PaymentRepository;
import in.jlenterprises.ecommerce.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The gateway webhook/reconciliation path added in the Razorpay batch: captured
 * payments settle idempotently, and a capture for an already-cancelled order is
 * auto-refunded instead of resurrecting the order.
 */
@ExtendWith(MockitoExtension.class)
class GatewayCaptureTest {

    @Mock OrderRepository orderRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock PaymentStrategyFactory strategyFactory;
    @Mock NotificationService notificationService;
    @Mock AccountingService accountingService;
    @Mock WhatsappAutomationService whatsappAutomation;
    @Mock PaymentStrategy razorpay;

    @InjectMocks PaymentServiceImpl paymentService;

    private static Payment payment(Order order, PaymentStatus status) {
        Payment p = new Payment();
        p.setMethod(PaymentMethod.RAZORPAY);
        p.setPaymentStatus(status);
        p.setAmount(new BigDecimal("2500.00"));
        p.setCurrency("INR");
        p.setOrder(order);
        p.setProviderPaymentId("order_abc123");
        return p;
    }

    private static Order order(OrderStatus status) throws Exception {
        Order o = new Order();
        o.setOrderStatus(status);
        o.setOrderNumber("JL1001");
        o.setCurrency("INR");
        User u = new User();
        u.setEmail("c@example.com");
        Field id = in.jlenterprises.ecommerce.entity.BaseEntity.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(u, UUID.randomUUID());
        id.set(o, UUID.randomUUID());
        o.setUser(u);
        return o;
    }

    private void wire(Payment p) {
        when(paymentRepository.findByProviderPaymentId("order_abc123")).thenReturn(Optional.of(p));
        when(paymentRepository.findByOrderIdForUpdate(p.getOrder().getId())).thenReturn(Optional.of(p));
    }

    @Test
    void captureSettlesAPendingPayment() throws Exception {
        Order o = order(OrderStatus.PENDING);
        Payment p = payment(o, PaymentStatus.PENDING);
        wire(p);

        paymentService.recordGatewayCapture("order_abc123", "pay_xyz");

        assertEquals(PaymentStatus.SUCCESS, p.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, o.getOrderStatus());
        assertTrue(p.getTransactions().stream().anyMatch(t ->
                t.getType() == TransactionType.CHARGE && "pay_xyz".equals(t.getProviderReference())));
        verify(paymentRepository).save(p);
    }

    @Test
    void captureIsIdempotentWhenAlreadySettled() throws Exception {
        Order o = order(OrderStatus.CONFIRMED);
        Payment p = payment(o, PaymentStatus.SUCCESS);
        wire(p);

        paymentService.recordGatewayCapture("order_abc123", "pay_xyz");

        // Webhooks retry — a second delivery must not duplicate the charge or the sale.
        assertTrue(p.getTransactions().isEmpty());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void captureForUnknownProviderOrderIsIgnored() {
        when(paymentRepository.findByProviderPaymentId("order_other")).thenReturn(Optional.empty());
        paymentService.recordGatewayCapture("order_other", "pay_x");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void captureAfterCancellationAutoRefunds() throws Exception {
        Order o = order(OrderStatus.CANCELLED);
        Payment p = payment(o, PaymentStatus.PENDING);
        wire(p);
        when(strategyFactory.forMethod(PaymentMethod.RAZORPAY)).thenReturn(razorpay);
        when(razorpay.refund(p)).thenReturn("rfnd_123");

        paymentService.recordGatewayCapture("order_abc123", "pay_xyz");

        // Money went straight back; the cancelled order stays cancelled.
        assertEquals(PaymentStatus.REFUNDED, p.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, o.getOrderStatus());
        assertTrue(p.getTransactions().stream().anyMatch(t ->
                t.getType() == TransactionType.REFUND && "rfnd_123".equals(t.getProviderReference())));
    }

    @Test
    void captureAfterCancellationAlertsWhenAutoRefundFails() throws Exception {
        Order o = order(OrderStatus.CANCELLED);
        Payment p = payment(o, PaymentStatus.PENDING);
        wire(p);
        when(strategyFactory.forMethod(PaymentMethod.RAZORPAY)).thenReturn(razorpay);
        when(razorpay.refund(p)).thenThrow(new RuntimeException("gateway down"));

        paymentService.recordGatewayCapture("order_abc123", "pay_xyz");

        // Charge stays on record (a customer IS out of money) and admins are notified.
        assertEquals(PaymentStatus.SUCCESS, p.getPaymentStatus());
        verify(notificationService).notifyAdmins(any(), org.mockito.ArgumentMatchers.contains("URGENT"),
                org.mockito.ArgumentMatchers.contains("refund"), any(), any(), any(), any());
    }

    // ── Webhook signature scheme (shared HMAC helper) ──

    @Test
    void webhookSignatureVerifies() {
        String body = "{\"event\":\"payment.captured\"}";
        String secret = "whsec_test";
        String sig = RazorpayPaymentStrategy.hmacSha256Hex(body, secret);

        assertNotNull(sig);
        assertTrue(RazorpayPaymentStrategy.constantTimeEquals(sig,
                RazorpayPaymentStrategy.hmacSha256Hex(body, secret)));
        assertFalse(RazorpayPaymentStrategy.constantTimeEquals(sig,
                RazorpayPaymentStrategy.hmacSha256Hex(body + " ", secret)));
        assertFalse(RazorpayPaymentStrategy.constantTimeEquals(sig,
                RazorpayPaymentStrategy.hmacSha256Hex(body, "wrong-secret")));
    }
}
