package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.dto.order.InvoiceDto;
import in.jlenterprises.ecommerce.dto.order.OrderDto;
import in.jlenterprises.ecommerce.dto.order.OrderSummaryDto;
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
import in.jlenterprises.ecommerce.mapper.OrderMapper;
import in.jlenterprises.ecommerce.repository.AddressRepository;
import in.jlenterprises.ecommerce.repository.CartRepository;
import in.jlenterprises.ecommerce.repository.InventoryRepository;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.repository.PaymentRepository;
import in.jlenterprises.ecommerce.request.order.PlaceOrderRequest;
import in.jlenterprises.ecommerce.service.CouponService;
import in.jlenterprises.ecommerce.service.NotificationService;
import in.jlenterprises.ecommerce.service.OrderService;
import in.jlenterprises.ecommerce.util.OrderNumberGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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

    public OrderServiceImpl(OrderRepository orderRepository, CartRepository cartRepository,
                            AddressRepository addressRepository, InventoryRepository inventoryRepository,
                            PaymentRepository paymentRepository, CouponService couponService,
                            NotificationService notificationService, OrderNumberGenerator orderNumberGenerator,
                            OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.inventoryRepository = inventoryRepository;
        this.paymentRepository = paymentRepository;
        this.couponService = couponService;
        this.notificationService = notificationService;
        this.orderNumberGenerator = orderNumberGenerator;
        this.orderMapper = orderMapper;
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

        BigDecimal subtotal = cart.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1) Verify stock for every line before touching anything.
        for (CartItem item : cart.getItems()) {
            Inventory inv = inventoryFor(item.getProduct());
            if (inv.getAvailable() < item.getQuantity()) {
                throw new BusinessException("Insufficient stock for " + item.getProduct().getName());
            }
        }
        // 2) Deduct (optimistic-lock guarded by @Version on Inventory).
        for (CartItem item : cart.getItems()) {
            Inventory inv = inventoryFor(item.getProduct());
            inv.setQuantity(inv.getQuantity() - item.getQuantity());
            inventoryRepository.save(inv);
        }

        // 3) Coupon.
        CouponService.AppliedCoupon applied = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            applied = couponService.apply(request.couponCode(), subtotal, userId);
            discount = applied.discount();
        }

        // 4) Totals.
        BigDecimal base = subtotal.subtract(discount);
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

        // 7) Record coupon usage, empty the cart, notify.
        if (applied != null) couponService.recordUsage(applied, user, saved);
        cart.getItems().clear();
        cartRepository.save(cart);
        notificationService.notifyUser(userId, NotificationType.ORDER, "Order placed",
                "Your order " + saved.getOrderNumber() + " has been placed.", "/orders/" + saved.getId());

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
    public OrderDto cancel(UUID userId, UUID orderId) {
        Order order = ownedOrder(userId, orderId);
        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("Order cannot be cancelled in status " + order.getOrderStatus());
        }
        restoreStock(order);
        order.setOrderStatus(OrderStatus.CANCELLED);
        if (order.getPayment() != null && order.getPayment().getPaymentStatus() == PaymentStatus.SUCCESS) {
            order.getPayment().setPaymentStatus(PaymentStatus.REFUNDED);
        }
        notificationService.notifyUser(userId, NotificationType.ORDER, "Order cancelled",
                "Your order " + order.getOrderNumber() + " has been cancelled.", "/orders/" + order.getId());
        return orderMapper.toDto(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto invoice(UUID userId, UUID orderId) {
        return orderMapper.toInvoice(ownedOrder(userId, orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> listAll(OrderStatus status, Pageable pageable) {
        Specification<Order> spec = status == null ? null
                : (root, query, cb) -> cb.equal(root.get("orderStatus"), status);
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
        if (current == OrderStatus.DELIVERED || current == OrderStatus.CANCELLED || current == OrderStatus.REFUNDED) {
            throw new BusinessException("Order is in a terminal state: " + current);
        }
        if (status == OrderStatus.CANCELLED) {
            restoreStock(order);
        }
        order.setOrderStatus(status);
        notificationService.notifyUser(order.getUser().getId(), NotificationType.ORDER,
                "Order update", "Your order " + order.getOrderNumber() + " is now " + status + ".",
                "/orders/" + order.getId());
        return orderMapper.toDto(orderRepository.save(order));
    }

    // ── helpers ──
    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getProduct() == null) continue;
            inventoryRepository.findByProductId(item.getProduct().getId()).ifPresent(inv -> {
                inv.setQuantity(inv.getQuantity() + item.getQuantity());
                inventoryRepository.save(inv);
            });
        }
    }

    private Inventory inventoryFor(Product product) {
        return inventoryRepository.findByProductId(product.getId())
                .orElseThrow(() -> new BusinessException("No stock record for " + product.getName()));
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
}
