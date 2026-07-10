package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;
import in.jlenterprises.ecommerce.entity.WhatsappMessageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsappMessageLogRepository extends JpaRepository<WhatsappMessageLog, UUID> {
    List<WhatsappMessageLog> findByCampaign_IdOrderByCreatedAtAsc(UUID campaignId);
    List<WhatsappMessageLog> findByCampaign_IdAndMessageStatus(UUID campaignId, WhatsappMessageStatus status);
    long countByCampaign_Id(UUID campaignId);
    /** Phase 2 webhook lookup. */
    Optional<WhatsappMessageLog> findByProviderMessageId(String providerMessageId);

    /** Filtered, paged delivery log across all campaigns. Any null filter is ignored. */
    @Query("select m from WhatsappMessageLog m where "
            + "(:status is null or m.messageStatus = :status) and "
            + "(:campaignId is null or m.campaign.id = :campaignId) and "
            + "(:phone is null or m.phone like concat('%', :phone, '%')) and "
            + "(:from is null or m.createdAt >= :from) and "
            + "(:to is null or m.createdAt <= :to)")
    Page<WhatsappMessageLog> search(@Param("status") WhatsappMessageStatus status,
                                    @Param("campaignId") UUID campaignId,
                                    @Param("phone") String phone,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to,
                                    Pageable pageable);
}
