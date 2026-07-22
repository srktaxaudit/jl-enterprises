package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.constant.TransactionType;
import in.jlenterprises.ecommerce.dto.order.OrderPaymentDto;
import in.jlenterprises.ecommerce.dto.payment.PaymentInitResponse;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.OrderItem;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.entity.Transaction;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.payment.PaymentConfirmation;
import in.jlenterprises.ecommerce.payment.PaymentInitResult;
import in.jlenterprises.ecommerce.payment.PaymentStrategy;
import in.jlenterprises.ecommerce.payment.PaymentStrategyFactory;
import in.jlenterprises.ecommerce.repository.InventoryRepository;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.repository.PaymentRepository;
import in.jlenterprises.ecommerce.request.payment.PaymentConfirmRequest;
import in.jlenterprises.ecommerce.constant.WhatsappAutomationEvent;
import in.jlenterprises.ecommerce.service.AccountingService;
import in.jlenterprises.ecommerce.service.CouponService;
import in.jlenterprises.ecommerce.service.NotificationService;
import in.jlenterprises.ecommerce.service.PaymentService;
import in.jlenterprises.ecommerce.service.WhatsappAutomationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStrategyFactory strategyFactory;
    private final NotificationService notificationService;
    private final AccountingService accountingService;
    private final WhatsappAutomationService whatsappAutomation;
    private final InventoryRepository inventoryRepository;
    private final CouponService couponService;
    private final in.jlenterprises.ecommerce.service.ExchangeService exchangeService;

    public PaymentServiceImpl(OrderRepository orderRepository, PaymentRepository paymentRepository,
                              PaymentStrategyFactory strategyFactory, NotificationService notificationService,
                              AccountingService accountingService, WhatsappAutomationService whatsappAutomation,
                              InventoryRepository inventoryRepository, CouponService couponService,
                              in.jlenterprises.ecommerce.service.ExchangeService exchangeService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.strategyFactory = strategyFactory;
        this.notificationService = notificationService;
        this.accountingService = accountingService;
        this.whatsappAutomation = whatsappAutomation;
        this.inventoryRepository = inventoryRepository;
        this.couponService = couponService;
        this.exchangeService = exchangeService;
    }

    /** After the current transaction commits, post the sale to the books (best-effort). */
    private void postSaleAfterCommit(UUID orderId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accountingService.postSaleForOrder(orderId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { accountingService.postSaleForOrder(orderId); }
        });
    }

    /** After the current transaction commits, reverse the sale on the books (best-effort). */
    private void reverseSaleAfterCommit(UUID orderId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            accountingService.reverseSaleForOrder(orderId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { accountingService.reverseSaleForOrder(orderId); }
        });
    }

    /** Put the ordered quantities back into stock, locking each row to match the deduct path. */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) continue;
            inventoryRepository.findByProductIdForUpdate(item.getProduct().getId()).ifPresent(inv -> {
                inv.setQuantity(inv.getQuantity() + item.getQuantity());
                inventoryRepository.save(inv);
            });
        }
    }

    @Override
    @Transactional
    public PaymentInitResponse initiate(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
        Payment payment = paymentFor(orderId);
        // A settled payment must never be re-initiated: doing so would mint a fresh provider
        // order, overwrite providerPaymentId (orphaning the original signature) and knock the
        // status back to PENDING — making a paid order look unpaid and payable a second time.
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS
                || payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException("This order's payment is already " + payment.getPaymentStatus()
                    + " and cannot be initiated again.");
        }
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("This order was cancelled. Please place a new order.");
        }
        PaymentStrategy strategy = strategyFactory.forMethod(payment.getMethod());

        PaymentInitResult result = strategy.initiate(payment);
        payment.setProvider(result.provider());
        payment.setProviderPaymentId(result.providerReference());
        if (result.status() != null) payment.setPaymentStatus(result.status());
        paymentRepository.save(payment);

        return new PaymentInitResponse(payment.getId(), payment.getMethod(), payment.getPaymentStatus(),
                result.provider(), result.providerReference(), result.clientData(),
                payment.getAmount(), payment.getCurrency());
    }

    @Override
    @Transactional
    public OrderPaymentDto confirm(UUID userId, UUID orderId, PaymentConfirmRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
        // Row-locked: two racing confirm callbacks would otherwise both read PENDING and
        // both record a CHARGE + post the sale. The second caller now blocks, then hits
        // the SUCCESS idempotency guard below.
        Payment payment = paymentForUpdate(orderId);

        // Idempotency: a payment already settled must not create a duplicate CHARGE
        // transaction or re-fire user/admin notifications on a repeated confirm callback.
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return toDto(payment);
        }
        // Cash on delivery is collected at delivery, not through this customer-facing confirm
        // endpoint. It stays PENDING until an admin marks the order fulfilled — otherwise a
        // customer could self-confirm their own COD order as paid.
        if (payment.getMethod() == PaymentMethod.COD) {
            return toDto(payment);
        }
        // The abandoned-order sweeper cancels (and RESTOCKS) orders whose payment never
        // confirmed. A confirm arriving after that must not mark the payment SUCCESS and post
        // a sale for stock that was already put back — surface it for manual follow-up instead.
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("This order was cancelled before the payment confirmation arrived. "
                    + "If you were charged, the amount will be refunded — please contact support.");
        }

        PaymentStrategy strategy = strategyFactory.forMethod(payment.getMethod());

        boolean paid = strategy.verify(payment,
                new PaymentConfirmation(request.providerReference(), request.signature(), request.payload()));

        if (paid) {
            settleOnline(order, payment, request.providerReference());
        } else {
            Transaction txn = new Transaction();
            txn.setPayment(payment);
            txn.setType(TransactionType.CHARGE);
            txn.setAmount(payment.getAmount());
            txn.setProviderReference(request.providerReference());
            txn.setProcessedAt(Instant.now());
            payment.setPaymentStatus(PaymentStatus.FAILED);
            txn.setTransactionStatus(PaymentStatus.FAILED);
            payment.getTransactions().add(txn);
            paymentRepository.save(payment);
        }

        return toDto(payment);
    }

    /** Mark a verified online payment SUCCESS: CHARGE txn, confirm the order, notify, post the sale.
        Shared by the customer confirm callback and the gateway webhook/reconciliation path. */
    private void settleOnline(Order order, Payment payment, String providerPaymentRef) {
        Transaction txn = new Transaction();
        txn.setPayment(payment);
        txn.setType(TransactionType.CHARGE);
        txn.setAmount(payment.getAmount());
        txn.setProviderReference(providerPaymentRef);
        txn.setProcessedAt(Instant.now());
        txn.setTransactionStatus(PaymentStatus.SUCCESS);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }
        notificationService.notifyUser(order.getUser().getId(), NotificationType.ORDER, "Payment received",
                "Payment for order " + order.getOrderNumber() + " was successful.", "/orders/" + order.getId());
        String payerName = order.getUser().getFullName();
        notificationService.notifyAdmins(NotificationType.PAYMENT, "Payment received",
                "Payment received from " + (payerName == null || payerName.isBlank() ? order.getUser().getEmail() : payerName)
                        + " for order " + order.getOrderNumber() + " (" + order.getCurrency() + " " + payment.getAmount() + ").",
                "/admin-orders.html", "Orders", order.getId(), "ORDER");
        whatsappAutomation.fire(WhatsappAutomationEvent.PAYMENT_RECEIVED, order);
        postSaleAfterCommit(order.getId());
        payment.getTransactions().add(txn);
        paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public void recordGatewayCapture(String providerOrderId, String providerPaymentId) {
        Payment lookup = paymentRepository.findByProviderPaymentId(providerOrderId).orElse(null);
        if (lookup == null) return;   // not ours (e.g. another environment's test event)
        // Re-read under the row lock so a racing customer confirm can't double-settle.
        Payment payment = paymentRepository.findByOrderIdForUpdate(lookup.getOrder().getId()).orElse(null);
        if (payment == null) return;
        Order order = payment.getOrder();

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS
                || payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return;   // already settled/handled — webhooks retry, so this must be idempotent
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            // Charged AFTER the order was cancelled and restocked (e.g. the sweeper won the
            // race). Give the money straight back rather than resurrecting the order.
            recordCaptureThenAutoRefund(order, payment, providerPaymentId);
            return;
        }

        settleOnline(order, payment, providerPaymentId);
    }

    /** The customer paid for an order that no longer exists commercially: record the charge,
        refund it at the gateway, and make sure a human hears about it either way. */
    private void recordCaptureThenAutoRefund(Order order, Payment payment, String providerPaymentId) {
        Transaction charge = new Transaction();
        charge.setPayment(payment);
        charge.setType(TransactionType.CHARGE);
        charge.setAmount(payment.getAmount());
        charge.setProviderReference(providerPaymentId);
        charge.setProcessedAt(Instant.now());
        charge.setTransactionStatus(PaymentStatus.SUCCESS);
        payment.getTransactions().add(charge);
        try {
            String refundRef = strategyFactory.forMethod(payment.getMethod()).refund(payment);
            Transaction refund = new Transaction();
            refund.setPayment(payment);
            refund.setType(TransactionType.REFUND);
            refund.setAmount(payment.getAmount());
            refund.setProviderReference(refundRef);
            refund.setProcessedAt(Instant.now());
            refund.setTransactionStatus(PaymentStatus.SUCCESS);
            payment.getTransactions().add(refund);
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            notificationService.notifyUser(order.getUser().getId(), NotificationType.ORDER, "Payment refunded",
                    "Your payment for cancelled order " + order.getOrderNumber()
                            + " has been refunded — it will reach your account in a few days.",
                    "/orders/" + order.getId());
            notificationService.notifyAdmins(NotificationType.PAYMENT, "Auto-refunded late payment",
                    "Order " + order.getOrderNumber() + " was already cancelled when its online payment "
                            + "arrived; the payment was auto-refunded at the gateway.",
                    "/admin-billing.html", "Billing", order.getId(), "ORDER");
        } catch (Exception e) {
            // Refund failed: keep the charge on record and SHOUT — a customer is out of money.
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            notificationService.notifyAdmins(NotificationType.PAYMENT, "URGENT: manual refund needed",
                    "Order " + order.getOrderNumber() + " was cancelled but its online payment captured, and the "
                            + "automatic refund FAILED (" + e.getMessage() + "). Refund it in the Razorpay dashboard.",
                    "/admin-billing.html", "Billing", order.getId(), "ORDER");
        }
        paymentRepository.save(payment);
    }

    @Override
    @Transactional
    @Auditable(action = "REFUND_PAYMENT", entity = "payment")
    public OrderPaymentDto refund(UUID orderId) {
        Payment payment = paymentForUpdate(orderId);
        Order order = payment.getOrder();
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessException("Only successful payments can be refunded");
        }
        // FIRST move the actual money: for RAZORPAY this calls the refund API and throws on
        // failure, so books/stock/status are never touched unless the gateway refund really
        // happened. COD returns null (staff hand the cash back — recorded as MANUAL).
        String gatewayRef = strategyFactory.forMethod(payment.getMethod()).refund(payment);

        // A refund reverses the sale: put stock back (locked), release the coupon usage, give
        // back any consumed trade-in credit, and reverse the sales journal after commit. The
        // SUCCESS guard above makes this idempotent — a second refund attempt fails, so
        // stock/coupons/credits are never restored twice.
        restoreStock(order);
        couponService.revokeForOrder(orderId);
        exchangeService.releaseFromOrder(orderId);

        Transaction txn = new Transaction();
        txn.setPayment(payment);
        txn.setType(TransactionType.REFUND);
        txn.setTransactionStatus(PaymentStatus.SUCCESS);
        txn.setAmount(payment.getAmount());
        txn.setProviderReference(gatewayRef == null ? "MANUAL" : gatewayRef);
        txn.setProcessedAt(Instant.now());
        payment.getTransactions().add(txn);

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setOrderStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);
        paymentRepository.save(payment);
        reverseSaleAfterCommit(orderId);

        notificationService.notifyUser(order.getUser().getId(), NotificationType.ORDER, "Refund issued",
                "A refund was issued for order " + order.getOrderNumber() + ".", "/orders/" + order.getId());
        return toDto(payment);
    }

    private Payment paymentFor(UUID orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("No payment associated with this order"));
    }

    private Payment paymentForUpdate(UUID orderId) {
        return paymentRepository.findByOrderIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException("No payment associated with this order"));
    }

    private OrderPaymentDto toDto(Payment p) {
        return new OrderPaymentDto(p.getMethod(), p.getPaymentStatus(), p.getAmount(),
                p.getCurrency(), p.getProviderPaymentId());
    }
}
