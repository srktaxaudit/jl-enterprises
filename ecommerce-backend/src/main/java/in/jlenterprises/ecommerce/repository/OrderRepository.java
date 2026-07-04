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
}
