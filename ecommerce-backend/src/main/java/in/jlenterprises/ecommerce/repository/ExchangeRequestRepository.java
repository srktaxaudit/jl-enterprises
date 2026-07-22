package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.ExchangeStatus;
import in.jlenterprises.ecommerce.entity.ExchangeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, UUID> {

    List<ExchangeRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<ExchangeRequest> findByUserId(UUID userId, Pageable pageable);

    /** Admin filter by status. */
    Page<ExchangeRequest> findByExchangeStatus(ExchangeStatus status, Pageable pageable);

    /** Exchanges still awaiting admin attention — for the sidebar count badge. */
    long countByExchangeStatusIn(Collection<ExchangeStatus> statuses);

    /** The exchange consumed by this order, if any — used to release it on cancellation. */
    java.util.Optional<ExchangeRequest> findByAppliedOrderId(UUID appliedOrderId);

    /**
     * Atomically consume the credit: succeeds (1 row) only if it is still unused. Two
     * concurrent checkouts spending the same exchange both pass the read-time check —
     * this conditional UPDATE is what guarantees only one of them wins.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(
            "update ExchangeRequest e set e.exchangeStatus = in.jlenterprises.ecommerce.constant.ExchangeStatus.COMPLETED, "
            + "e.appliedOrderId = :orderId where e.id = :id and e.appliedOrderId is null")
    int consumeIfUnused(@org.springframework.data.repository.query.Param("id") UUID id,
                        @org.springframework.data.repository.query.Param("orderId") UUID orderId);
}
