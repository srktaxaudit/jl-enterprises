package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.config.BillingConfig;
import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.constant.TransactionType;
import in.jlenterprises.ecommerce.dto.admin.BillingRowDto;
import in.jlenterprises.ecommerce.dto.admin.BillingSummaryDto;
import in.jlenterprises.ecommerce.dto.order.InvoiceDto;
import in.jlenterprises.ecommerce.dto.order.OrderPaymentDto;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.entity.Transaction;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.OrderMapper;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.repository.PaymentRepository;
import in.jlenterprises.ecommerce.service.AccountingService;
import in.jlenterprises.ecommerce.service.BillingService;
import in.jlenterprises.ecommerce.util.GstUtil;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
public class BillingServiceImpl implements BillingService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;
    private final BillingConfig billingConfig;
    private final AccountingService accountingService;

    public BillingServiceImpl(OrderRepository orderRepository, PaymentRepository paymentRepository,
                              OrderMapper orderMapper, BillingConfig billingConfig,
                              AccountingService accountingService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderMapper = orderMapper;
        this.billingConfig = billingConfig;
        this.accountingService = accountingService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BillingRowDto> listInvoices(PaymentStatus status, PaymentMethod method, String q,
                                            Instant from, Instant to, Pageable pageable) {
        BigDecimal rate = billingConfig.gstRate();
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            var payment = root.join("payment", JoinType.LEFT);
            if (status != null) ps.add(cb.equal(payment.get("paymentStatus"), status));
            if (method != null) ps.add(cb.equal(payment.get("method"), method));
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("placedAt"), from));
            if (to != null) ps.add(cb.lessThanOrEqualTo(root.get("placedAt"), to));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                var user = root.join("user", JoinType.LEFT);
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), like),
                        cb.like(cb.lower(user.get("email")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return orderRepository.findAll(spec, pageable).map(o -> toRow(o, rate));
    }

    @Override
    @Transactional(readOnly = true)
    public BillingSummaryDto summary(Instant from, Instant to) {
        BigDecimal rate = billingConfig.gstRate();
        List<Order> orders = orderRepository.findWithPaymentBetween(from, to);
        BigDecimal gross = BigDecimal.ZERO, tax = BigDecimal.ZERO, paid = BigDecimal.ZERO,
                pending = BigDecimal.ZERO, refunded = BigDecimal.ZERO, cod = BigDecimal.ZERO, online = BigDecimal.ZERO;
        long paidCount = 0, pendingCount = 0, refundedCount = 0;

        for (Order o : orders) {
            BigDecimal grand = o.getGrandTotal() == null ? BigDecimal.ZERO : o.getGrandTotal();
            if (o.getOrderStatus() != OrderStatus.CANCELLED) gross = gross.add(grand);
            Payment p = o.getPayment();
            PaymentStatus ps = p == null ? PaymentStatus.PENDING : p.getPaymentStatus();
            PaymentMethod pm = p == null ? null : p.getMethod();
            switch (ps) {
                case SUCCESS -> { paid = paid.add(grand); tax = tax.add(GstUtil.gstAmount(grand, rate)); paidCount++; }
                case REFUNDED -> { refunded = refunded.add(grand); refundedCount++; }
                default -> { pending = pending.add(grand); pendingCount++; }
            }
            if (pm == PaymentMethod.COD) cod = cod.add(grand);
            else if (pm != null) online = online.add(grand);
        }
        return new BillingSummaryDto(from, to, orders.size(), gross, tax, paid.subtract(refunded),
                paid, pending, refunded, cod, online, paidCount, pendingCount, refundedCount, rate);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto adminInvoice(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
        return orderMapper.toInvoice(order, billingConfig.gstRate(), billingConfig.sellerGstin(),
                billingConfig.sellerName(), billingConfig.sellerAddress());
    }

    @Override
    @Transactional
    @Auditable(action = "MARK_PAID", entity = "payment")
    public OrderPaymentDto markPaid(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("No payment associated with this order"));
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return toDto(payment);   // already paid — idempotent
        }
        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException("A refunded payment cannot be marked as paid");
        }
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        Order order = payment.getOrder();
        if (order.getOrderStatus() == OrderStatus.PENDING) {
            order.setOrderStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }
        Transaction txn = new Transaction();
        txn.setPayment(payment);
        txn.setType(TransactionType.CHARGE);
        txn.setTransactionStatus(PaymentStatus.SUCCESS);
        txn.setAmount(payment.getAmount());
        txn.setProviderReference("MANUAL");
        txn.setProcessedAt(Instant.now());
        payment.getTransactions().add(txn);
        paymentRepository.save(payment);

        // Post the sale to the books once this payment change is committed (best-effort).
        final UUID oid = order.getId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { accountingService.postSaleForOrder(oid); }
            });
        }
        return toDto(payment);
    }

    // ── helpers ──
    private BillingRowDto toRow(Order o, BigDecimal rate) {
        User u = o.getUser();
        Payment p = o.getPayment();
        BigDecimal grand = o.getGrandTotal() == null ? BigDecimal.ZERO : o.getGrandTotal();
        return new BillingRowDto(
                o.getId(), o.getOrderNumber(),
                u == null ? null : displayName(u), u == null ? null : u.getEmail(),
                o.getPlacedAt(), o.getOrderStatus() == null ? null : o.getOrderStatus().name(),
                p == null ? null : p.getMethod().name(),
                p == null ? "PENDING" : p.getPaymentStatus().name(),
                grand, GstUtil.gstAmount(grand, rate), o.getCurrency());
    }

    private String displayName(User u) {
        String full = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return full.isBlank() ? u.getEmail() : full;
    }

    private OrderPaymentDto toDto(Payment p) {
        return new OrderPaymentDto(p.getMethod(), p.getPaymentStatus(), p.getAmount(),
                p.getCurrency(), p.getProviderPaymentId());
    }
}
