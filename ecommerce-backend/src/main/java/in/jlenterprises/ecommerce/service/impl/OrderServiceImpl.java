package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.constant.WhatsappAutomationEvent;
import in.jlenterprises.ecommerce.dto.order.InvoiceDto;
import in.jlenterprises.ecommerce.dto.order.OrderDto;
import in.jlenterprises.ecommerce.dto.order.OrderSummaryDto;
import in.jlenterprises.ecommerce.dto.order.OrderTrackingDto;
import in.jlenterprises.ecommerce.entity.Address;
import in.jlenterprises.ecommerce.entity.AddressSnapshot;
import in.jlenterprises.ecommerce.entity.Cart;
import in.jlenterprises.ecommerce.entity.CartItem;
import in.jlenterprises.ecommerce.entity.Inventory;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.OrderItem;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.config.BillingConfig;
import in.jlenterprises.ecommerce.mapper.OrderMapper;
import in.jlenterprises.ecommerce.repository.AddressRepository;
import in.jlenterprises.ecommerce.repository.CartRepository;
import in.jlenterprises.ecommerce.repository.InventoryRepository;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.repository.PaymentRepository;
import in.jlenterprises.ecommerce.request.order.PlaceOrderRequest;
import in.jlenterprises.ecommerce.service.AccountingService;
import in.jlenterprises.ecommerce.service.CouponService;
import in.jlenterprises.ecommerce.service.NotificationService;
import in.jlenterprises.ecommerce.service.OrderService;
import in.jlenterprises.ecommerce.service.WhatsappAutomationService;
import in.jlenterprises.ecommerce.util.OrderNumberGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("5000");
    private static final BigDecimal FLAT_SHIPPING = new BigDecimal("99");

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final CouponService couponService;
    private final NotificationService notificationService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderMapper orderMapper;
    private final BillingConfig billingConfig;
    private final in.jlenterprises.ecommerce.service.ExchangeService exchangeService;
    private final WhatsappAutomationService whatsappAutomation;
    private final AccountingService accountingService;

    public OrderServiceImpl(OrderRepository orderRepository, CartRepository cartRepository,
                            AddressRepository addressRepository, InventoryRepository inventoryRepository,
                            PaymentRepository paymentRepository, CouponService couponService,
                            NotificationService notificationService, OrderNumberGenerator orderNumberGenerator,
                            OrderMapper orderMapper, BillingConfig billingConfig,
                            in.jlenterprises.ecommerce.service.ExchangeService exchangeService,
                            WhatsappAutomationService whatsappAutomation, AccountingService accountingService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.inventoryRepository = inventoryRepository;
        this.paymentRepository = paymentRepository;
        this.couponService = couponService;
        this.notificationService = notificationService;
        this.orderNumberGenerator = orderNumberGenerator;
        this.orderMapper = orderMapper;
        this.billingConfig = billingConfig;
        this.exchangeService = exchangeService;
        this.whatsappAutomation = whatsappAutomation;
        this.accountingService = accountingService;
    }

    @Override
    @Transactional
    public OrderDto placeOrder(UUID userId, PlaceOrderRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Your cart is empty"));
        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Your cart is empty");
        }
        User user = cart.getUser();

        Address shipping = addressRepository.findByIdAndUserId(request.shippingAddressId(), userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Address", request.shippingAddressId()));
        Address billing = request.billingAddressId() == null ? shipping
                : addressRepository.findByIdAndUserId(request.billingAddressId(), userId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Address", request.billingAddressId()));

        // 2) Final gate against the CURRENT catalogue. Cart lines refresh their price on every
        // add/sync (CartServiceImpl.addItem), so a mismatch here means the price or status
        // changed between the checkout page loading and the customer clicking Place Order.
        // Refuse rather than silently charging an amount the checkout page never showed —
        // the retry re-syncs the cart and picks up the current price.
        for (CartItem item : cart.getItems()) {
            var product = item.getProduct();
            if (product == null || product.getStatus() != in.jlenterprises.ecommerce.constant.RecordStatus.ACTIVE) {
                throw new BusinessException("\"" + (product == null ? "An item" : product.getName())
                        + "\" is no longer available. Please remove it from your cart.");
            }
            BigDecimal current = item.getVariant() != null && item.getVariant().getPrice() != null
                    ? item.getVariant().getPrice() : product.getPrice();
            if (current != null && item.getUnitPrice() != null && item.getUnitPrice().compareTo(current) != 0) {
                throw new BusinessException("The price of \"" + product.getName()
                        + "\" changed while you were checking out. Please review your cart and try again.");
            }
        }

        BigDecimal subtotal = cart.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Verify + deduct stock under a per-row write lock so two concurrent
        // checkouts of the last unit can't both succeed (overselling). Each
        // inventory row is locked (SELECT ... FOR UPDATE), re-checked against the
        // freshest quantity, then decremented within this same transaction.
        for (CartItem item : cart.getItems()) {
            Inventory inv = inventoryRepository.findByProductIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new BusinessException(
                            "Product is unavailable: " + item.getProduct().getName()));
            if (inv.getAvailable() < item.getQuantity()) {
                throw new BusinessException("Insufficient stock for " + item.getProduct().getName());
            }
            inv.setQuantity(inv.getQuantity() - item.getQuantity());
            inventoryRepository.save(inv);
        }

        // 3) Coupon.
        CouponService.AppliedCoupon applied = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            // Revalidate from persisted cart lines and server-side prices. The
            // discount is computed only over category-eligible items.
            applied = couponService.apply(request.couponCode(), cart.getItems(), userId);
            discount = applied.discount();
        }

        // 3b) Exchange (trade-in) credit. Validated against the user + approved value;
        // capped so it never exceeds the post-coupon merchandise total.
        BigDecimal exchangeValue = BigDecimal.ZERO;
        if (request.exchangeRequestId() != null) {
            BigDecimal credit = exchangeService.valueForCheckout(userId, request.exchangeRequestId());
            exchangeValue = credit.min(subtotal.subtract(discount).max(BigDecimal.ZERO));
        }

        // 4) Totals.
        BigDecimal base = subtotal.subtract(discount).subtract(exchangeValue);
        if (base.signum() < 0) base = BigDecimal.ZERO;
        BigDecimal shipping_ = base.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : FLAT_SHIPPING;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal grand = base.add(tax).add(shipping_);

        // 5) Build the order + items.
        Order order = new Order();
        order.setOrderNumber(orderNumberGenerator.generate());
        order.setUser(user);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setSubtotal(subtotal);
        order.setDiscountTotal(discount);
        order.setTaxTotal(tax);
        order.setShippingTotal(shipping_);
        order.setGrandTotal(grand);
        order.setCurrency("INR");
        order.setExchangeValue(exchangeValue);
        if (request.exchangeRequestId() != null && exchangeValue.signum() > 0) {
            order.setExchangeRequestId(request.exchangeRequestId());
        }
        if (applied != null) order.setCoupon(applied.coupon());
        order.setShippingAddress(snapshot(shipping));
        order.setBillingAddress(snapshot(billing));
        order.setNotes(request.notes());
        order.setPlacedAt(Instant.now());

        for (CartItem item : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(item.getProduct());
            oi.setVariant(item.getVariant());
            oi.setProductName(item.getProduct().getName());
            oi.setSku(item.getProduct().getSku());
            oi.setUnitPrice(item.getUnitPrice());
            oi.setQuantity(item.getQuantity());
            oi.setLineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            order.getItems().add(oi);
        }
        Order saved = orderRepository.save(order);

        // 6) Payment (pending until the payment module processes it).
        Payment payment = new Payment();
        payment.setOrder(saved);
        payment.setMethod(request.paymentMethod());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setAmount(grand);
        payment.setCurrency("INR");
        paymentRepository.save(payment);
        saved.setPayment(payment);

        // 7) Record coupon usage, consume the exchange, empty the cart, notify.
        if (applied != null) couponService.recordUsage(applied, user, saved);
        if (saved.getExchangeRequestId() != null) {
            exchangeService.applyToOrder(saved.getExchangeRequestId(), saved.getId());
        }
        cart.getItems().clear();
        cartRepository.save(cart);
        notificationService.notifyUser(userId, NotificationType.ORDER, "Order placed",
                "Your order " + saved.getOrderNumber() + " has been placed.", "/orders/" + saved.getId());
        notificationService.notifyAdmins(NotificationType.ORDER, "New order placed",
                "Order " + saved.getOrderNumber() + " for " + saved.getCurrency() + " " + saved.getGrandTotal()
                        + " was placed by " + user.getEmail() + ".", "/admin-orders.html");
        whatsappAutomation.fire(WhatsappAutomationEvent.ORDER_PLACED, saved);

        return orderMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> myOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(orderMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto myOrder(UUID userId, UUID orderId) {
        return orderMapper.toDto(ownedOrder(userId, orderId));
    }

    @Override
    @Transactional
    public OrderDto cancel(UUID userId, UUID orderId, String reason) {
        Order order = ownedOrder(userId, orderId);
        OrderStatus s = order.getOrderStatus();
        if (s != OrderStatus.PENDING && s != OrderStatus.CONFIRMED && s != OrderStatus.PROCESSING) {
            throw new BusinessException("This order can no longer be cancelled (status: " + s + ").");
        }
        doCancel(order, reason);
        notificationService.notifyUser(userId, NotificationType.ORDER, "Order cancelled",
                "Your order " + order.getOrderNumber() + " has been cancelled.", "/orders/" + order.getId());
        notificationService.notifyAdmins(NotificationType.ORDER, "Product order cancelled",
                "Product order " + order.getOrderNumber() + " was cancelled by " + who(order.getUser()) + ".",
                "/admin-orders.html", "Orders", order.getId(), "ORDER");
        return orderMapper.toDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public int expireAbandonedOrders(java.time.Duration olderThan) {
        Instant cutoff = Instant.now().minus(olderThan);
        java.util.List<Order> abandoned = orderRepository.findAbandonedOnlineOrders(cutoff);
        for (Order order : abandoned) {
            doCancel(order, "Auto-cancelled: online payment was not completed in time.");
            notificationService.notifyAdmins(NotificationType.ORDER, "Order auto-cancelled (unpaid)",
                    "Order " + order.getOrderNumber() + " was auto-cancelled and its stock released — "
                            + "the online payment was not completed.",
                    "/admin-orders.html", "Orders", order.getId(), "ORDER");
            whatsappAutomation.fire(WhatsappAutomationEvent.ABANDONED_CHECKOUT, order);
        }
        if (!abandoned.isEmpty()) orderRepository.saveAll(abandoned);
        return abandoned.size();
    }

    @Override
    @Transactional
    public OrderDto requestReturn(UUID userId, UUID orderId, String reason) {
        Order order = ownedOrder(userId, orderId);
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Only delivered orders can be returned.");
        }
        order.setOrderStatus(OrderStatus.RETURN_REQUESTED);
        order.setReturnReason(reason);
        order.setReturnRequestedAt(Instant.now());
        notificationService.notifyUser(userId, NotificationType.ORDER, "Return requested",
                "We've received your return request for order " + order.getOrderNumber() + ".", "/orders/" + order.getId());
        notificationService.notifyAdmins(NotificationType.ORDER, "Return requested",
                who(order.getUser()) + " requested a return for order " + order.getOrderNumber() + ".",
                "/admin-orders.html", "Orders", order.getId(), "ORDER");
        return orderMapper.toDto(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto invoice(UUID userId, UUID orderId) {
        return orderMapper.toInvoice(ownedOrder(userId, orderId), billingConfig.gstRate(),
                billingConfig.sellerGstin(), billingConfig.sellerName(), billingConfig.sellerAddress());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingDto track(String orderNumber, String phone) {
        // Generic "not found" for both a missing order and a phone mismatch, so the
        // endpoint can't be used to enumerate order numbers.
        Order order = orderRepository.findByOrderNumber(orderNumber == null ? "" : orderNumber.trim())
                .filter(o -> o.getShippingAddress() != null
                        && last10(o.getShippingAddress().getPhone()).equals(last10(phone)))
                .orElseThrow(() -> new BusinessException("No order found for that number and phone."));
        return new OrderTrackingDto(order.getOrderNumber(), order.getOrderStatus(), order.getPlacedAt());
    }

    /** Last 10 digits of a phone number, for tolerant matching (+91/spaces ignored). */
    private static String last10(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> listAll(OrderStatus status, PaymentStatus paymentStatus,
                                         Instant from, Instant to, Pageable pageable) {
        Specification<Order> spec = Specification.where(null);
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("orderStatus"), status));
        }
        if (paymentStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.join("payment").get("paymentStatus"), paymentStatus));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("placedAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("placedAt"), to));
        }
        return orderRepository.findAll(spec, pageable).map(orderMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> userOrders(UUID userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(orderMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto adminGet(UUID orderId) {
        return orderMapper.toDto(getEntity(orderId));
    }

    @Override
    @Transactional
    @Auditable(action = "CHANGE_ORDER_STATUS", entity = "order")
    public OrderDto updateStatus(UUID orderId, OrderStatus status) {
        Order order = getEntity(orderId);
        OrderStatus current = order.getOrderStatus();
        if (!isTransitionAllowed(current, status)) {
            throw new BusinessException("Cannot change status from " + current + " to " + status + ".");
        }
        if (status == OrderStatus.CANCELLED) {
            doCancel(order, "Cancelled by staff");
        } else if (status == OrderStatus.RETURNED) {
            refundAndRestore(order);
            order.setOrderStatus(status);
        } else {
            order.setOrderStatus(status);
        }
        notificationService.notifyUser(order.getUser().getId(), NotificationType.ORDER,
                "Order update", "Your order " + order.getOrderNumber() + " is now " + status + ".",
                "/orders/" + order.getId());
        WhatsappAutomationEvent event = switch (status) {
            case SHIPPED -> WhatsappAutomationEvent.ORDER_SHIPPED;
            case OUT_FOR_DELIVERY -> WhatsappAutomationEvent.ORDER_OUT_FOR_DELIVERY;
            case DELIVERED -> WhatsappAutomationEvent.ORDER_DELIVERED;
            case CANCELLED -> WhatsappAutomationEvent.ORDER_CANCELLED;
            default -> null;
        };
        if (event != null) whatsappAutomation.fire(event, order);
        return orderMapper.toDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    @Auditable(action = "APPROVE_RETURN", entity = "order")
    public OrderDto approveReturn(UUID orderId) {
        Order order = getEntity(orderId);
        if (order.getOrderStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new BusinessException("There is no pending return request for this order.");
        }
        refundAndRestore(order);
        order.setOrderStatus(OrderStatus.RETURNED);
        notificationService.notifyUser(order.getUser().getId(), NotificationType.ORDER, "Return approved",
                "Your return for order " + order.getOrderNumber() + " was approved; refund is being processed.",
                "/orders/" + order.getId());
        return orderMapper.toDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    @Auditable(action = "REJECT_RETURN", entity = "order")
    public OrderDto rejectReturn(UUID orderId, String reason) {
        Order order = getEntity(orderId);
        if (order.getOrderStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new BusinessException("There is no pending return request for this order.");
        }
        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setAdminNotes(appendNote(order.getAdminNotes(), "Return rejected: " + (reason == null ? "" : reason)));
        notificationService.notifyUser(order.getUser().getId(), NotificationType.ORDER, "Return declined",
                "Your return request for order " + order.getOrderNumber() + " was declined.", "/orders/" + order.getId());
        return orderMapper.toDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto setAdminNotes(UUID orderId, String notes) {
        Order order = getEntity(orderId);
        order.setAdminNotes(notes);
        return orderMapper.toDto(orderRepository.save(order));
    }

    // ── helpers ──

    /** Allowed status transitions (admin). Terminal states have no outgoing edges. */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.CANCELLED, OrderStatus.FAILED_PAYMENT),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.PACKED, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.PACKED, OrderStatus.CANCELLED),
            OrderStatus.PACKED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED),
            OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED),
            OrderStatus.DELIVERED, Set.of(OrderStatus.RETURN_REQUESTED),
            OrderStatus.RETURN_REQUESTED, Set.of(OrderStatus.RETURNED, OrderStatus.DELIVERED),
            OrderStatus.RETURNED, Set.of(OrderStatus.REFUNDED));

    private boolean isTransitionAllowed(OrderStatus from, OrderStatus to) {
        return from != to && TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    /** Cancel: restock, revoke coupon usage, set reason/timestamp, mark payment refunded if paid. */
    private void doCancel(Order order, String reason) {
        refundAndRestore(order);
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setCancellationReason(reason);
    }

    /** Restore inventory + coupon usage, flip a successful payment to REFUNDED, and reverse the
        sales journal after commit (best-effort, idempotent) so the books don't overstate revenue. */
    private void refundAndRestore(Order order) {
        restoreStock(order);
        couponService.revokeForOrder(order.getId());
        // A consumed trade-in credit belongs to the customer, not the order — give it back.
        exchangeService.releaseFromOrder(order.getId());
        if (order.getPayment() != null && order.getPayment().getPaymentStatus() == PaymentStatus.SUCCESS) {
            order.getPayment().setPaymentStatus(PaymentStatus.REFUNDED);
        }
        reverseSaleAfterCommit(order.getId());
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

    private static String appendNote(String existing, String add) {
        return (existing == null || existing.isBlank()) ? add : existing + "\n" + add;
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) continue;
            // Lock the row (SELECT ... FOR UPDATE) to match the deduct path, so a restock
            // and a concurrent checkout of the same product can't race on the quantity.
            inventoryRepository.findByProductIdForUpdate(item.getProduct().getId()).ifPresent(inv -> {
                inv.setQuantity(inv.getQuantity() + item.getQuantity());
                inventoryRepository.save(inv);
            });
        }
    }

    private AddressSnapshot snapshot(Address a) {
        AddressSnapshot s = new AddressSnapshot();
        s.setFullName(a.getFullName());
        s.setPhone(a.getPhone());
        s.setLine1(a.getLine1());
        s.setLine2(a.getLine2());
        s.setCity(a.getCity());
        s.setState(a.getState());
        s.setPostalCode(a.getPostalCode());
        s.setCountry(a.getCountry());
        return s;
    }

    private Order ownedOrder(UUID userId, UUID orderId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
    }

    private Order getEntity(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
    }

    /** Display name for admin notifications — full name if present, else email. */
    private static String who(User u) {
        if (u == null) return "a customer";
        String name = u.getFullName();
        return (name == null || name.isBlank()) ? u.getEmail() : name;
    }
}
