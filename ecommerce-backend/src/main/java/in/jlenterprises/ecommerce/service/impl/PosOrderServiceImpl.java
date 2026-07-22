package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.constant.RoleName;
import in.jlenterprises.ecommerce.dto.order.OrderDto;
import in.jlenterprises.ecommerce.entity.AddressSnapshot;
import in.jlenterprises.ecommerce.entity.Inventory;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.OrderItem;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.entity.Role;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.OrderMapper;
import in.jlenterprises.ecommerce.repository.InventoryRepository;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.repository.PaymentRepository;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import in.jlenterprises.ecommerce.repository.RoleRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.request.admin.PosOrderRequest;
import in.jlenterprises.ecommerce.service.AccountingService;
import in.jlenterprises.ecommerce.service.NotificationService;
import in.jlenterprises.ecommerce.service.PosOrderService;
import in.jlenterprises.ecommerce.util.OrderNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PosOrderServiceImpl implements PosOrderService {

    private static final Logger log = LoggerFactory.getLogger(PosOrderServiceImpl.class);

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;
    private final AccountingService accountingService;
    private final in.jlenterprises.ecommerce.config.BillingConfig billingConfig;

    public PosOrderServiceImpl(ProductRepository productRepository, InventoryRepository inventoryRepository,
                               OrderRepository orderRepository, PaymentRepository paymentRepository,
                               UserRepository userRepository, RoleRepository roleRepository,
                               OrderNumberGenerator orderNumberGenerator, OrderMapper orderMapper,
                               NotificationService notificationService, AccountingService accountingService,
                               in.jlenterprises.ecommerce.config.BillingConfig billingConfig) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.orderNumberGenerator = orderNumberGenerator;
        this.orderMapper = orderMapper;
        this.notificationService = notificationService;
        this.accountingService = accountingService;
        this.billingConfig = billingConfig;
    }

    @Override
    @Transactional
    public OrderDto createPosOrder(PosOrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException("Add at least one product to the sale.");
        }
        String last10 = last10(request.customerPhone());
        if (last10.length() != 10) throw new BusinessException("Enter a valid 10-digit customer mobile number.");

        User customer = resolveCustomer(request.customerName().trim(), last10);

        // Build line items and deduct stock under a per-row write lock (matches checkout).
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> lines = new ArrayList<>();
        for (PosOrderRequest.Line line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", line.productId()));
            int qty = Math.max(1, line.quantity());
            Inventory inv = inventoryRepository.findByProductIdForUpdate(product.getId())
                    .orElseThrow(() -> new BusinessException("No stock record for " + product.getName()));
            if (inv.getAvailable() < qty) {
                throw new BusinessException("Insufficient stock for " + product.getName()
                        + " (" + inv.getAvailable() + " available).");
            }
            inv.setQuantity(inv.getQuantity() - qty);
            inventoryRepository.save(inv);

            BigDecimal unit = (line.unitPrice() != null && line.unitPrice().signum() >= 0)
                    ? line.unitPrice() : product.getPrice();
            OrderItem oi = new OrderItem();
            oi.setProduct(product);
            oi.setProductName(product.getName());
            oi.setSku(product.getSku());
            oi.setUnitPrice(unit);
            oi.setQuantity(qty);
            oi.setLineTotal(unit.multiply(BigDecimal.valueOf(qty)));
            // Same GST snapshot as online orders — invoices/books must not follow later catalogue edits.
            oi.setGstRate(product.getGstRate() != null ? product.getGstRate() : billingConfig.gstRate());
            oi.setHsnCode(product.getHsnCode());
            lines.add(oi);
            subtotal = subtotal.add(oi.getLineTotal());
        }

        BigDecimal discount = (request.discount() != null && request.discount().signum() > 0)
                ? request.discount() : BigDecimal.ZERO;
        if (discount.compareTo(subtotal) > 0) discount = subtotal;
        BigDecimal grand = subtotal.subtract(discount);

        // Completed counter sale.
        Order order = new Order();
        order.setOrderNumber(orderNumberGenerator.generate());
        order.setUser(customer);
        order.setOrderStatus(OrderStatus.DELIVERED);
        order.setSubtotal(subtotal);
        order.setDiscountTotal(discount);
        order.setTaxTotal(BigDecimal.ZERO);
        order.setShippingTotal(BigDecimal.ZERO);
        order.setGrandTotal(grand);
        order.setCurrency("INR");
        order.setExchangeValue(BigDecimal.ZERO);
        AddressSnapshot who = new AddressSnapshot();
        who.setFullName(request.customerName().trim());
        who.setPhone("+91" + last10);
        order.setShippingAddress(who);
        order.setBillingAddress(who);
        order.setNotes(request.notes());
        order.setAdminNotes("Counter / POS sale");
        order.setPlacedAt(Instant.now());
        for (OrderItem oi : lines) { oi.setOrder(order); order.getItems().add(oi); }
        Order saved = orderRepository.save(order);

        // Cash collected → payment marked paid (COD provider, SUCCESS).
        Payment payment = new Payment();
        payment.setOrder(saved);
        payment.setMethod(PaymentMethod.COD);
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setAmount(grand);
        payment.setCurrency("INR");
        paymentRepository.save(payment);
        saved.setPayment(payment);

        // Post the sale to the books after commit (idempotent, best-effort).
        UUID orderId = saved.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    try { accountingService.postSaleForOrder(orderId); }
                    catch (Exception e) { log.warn("POS sale {} accounting post failed: {}", orderId, e.getMessage()); }
                }
            });
        }

        notificationService.notifyAdmins(NotificationType.ORDER, "Counter sale recorded",
                "POS sale " + saved.getOrderNumber() + " for " + request.customerName().trim()
                        + " (" + saved.getCurrency() + " " + grand + ").",
                "/admin-orders.html", "Orders", saved.getId(), "ORDER");

        return orderMapper.toDto(saved);
    }

    /** Reuse a customer matched by phone, else create a lightweight walk-in customer. */
    private User resolveCustomer(String name, String last10) {
        User existing = userRepository.findByPhoneLast10(last10).stream().findFirst().orElse(null);
        if (existing != null) return existing;
        User u = new User();
        u.setFirstName(name.isEmpty() ? "Walk-in customer" : name);
        u.setPhone("+91" + last10);
        u.setEmail("pos-" + last10 + "@walkin.jlstores.in");   // synthetic, unique — no login
        u.setEmailVerified(false);
        u.setEnabled(true);
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new BusinessException("Customer role missing"));
        u.getRoles().add(customerRole);
        return userRepository.save(u);
    }

    private static String last10(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }
}
