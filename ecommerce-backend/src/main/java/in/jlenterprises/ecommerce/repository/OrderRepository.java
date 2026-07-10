package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    long countByOrderStatus(OrderStatus status);

    // ── Analytics ──
    @Query("select coalesce(sum(o.grandTotal), 0) from Order o " +
           "where o.orderStatus <> in.jlenterprises.ecommerce.constant.OrderStatus.CANCELLED " +
           "and o.placedAt >= :from")
    BigDecimal revenueSince(@Param("from") Instant from);

    @Query("select count(o) from Order o where o.placedAt >= :from")
    long countPlacedSince(@Param("from") Instant from);

    /** Orders in a date range with the payment eagerly loaded — for the billing summary. */
    @Query("select o from Order o left join fetch o.payment where o.placedAt between :from and :to")
    List<Order> findWithPaymentBetween(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * Abandoned online orders: still PENDING, placed before the cutoff, whose payment is a
     * gateway (non-COD) method that never succeeded. COD orders are excluded — they stay
     * PENDING legitimately until delivery — so the sweeper never cancels a real COD order.
     */
    @Query("select o from Order o where o.orderStatus = in.jlenterprises.ecommerce.constant.OrderStatus.PENDING "
            + "and o.placedAt < :cutoff and o.payment is not null "
            + "and o.payment.method <> in.jlenterprises.ecommerce.constant.PaymentMethod.COD "
            + "and o.payment.paymentStatus <> in.jlenterprises.ecommerce.constant.PaymentStatus.SUCCESS")
    List<Order> findAbandonedOnlineOrders(@Param("cutoff") Instant cutoff);
}
