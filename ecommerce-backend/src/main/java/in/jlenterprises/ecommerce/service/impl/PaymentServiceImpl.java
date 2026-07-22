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
        Payment payment = paymentFor(orderId);

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

        Transaction txn = new Transaction();
        txn.setPayment(payment);
        txn.setType(TransactionType.CHARGE);
        txn.setAmount(payment.getAmount());
        txn.setProviderReference(request.providerReference());
        txn.setProcessedAt(Instant.now());

        if (paid) {
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            txn.setTransactionStatus(PaymentStatus.SUCCESS);
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
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            txn.setTransactionStatus(PaymentStatus.FAILED);
        }
        payment.getTransactions().add(txn);
        paymentRepository.save(payment);

        return toDto(payment);
    }

    @Override
    @Transactional
    @Auditable(action = "REFUND_PAYMENT", entity = "payment")
    public OrderPaymentDto refund(UUID orderId) {
        Payment payment = paymentFor(orderId);
        Order order = payment.getOrder();
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessException("Only successful payments can be refunded");
        }
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

    private OrderPaymentDto toDto(Payment p) {
        return new OrderPaymentDto(p.getMethod(), p.getPaymentStatus(), p.getAmount(),
                p.getCurrency(), p.getProviderPaymentId());
    }
}
