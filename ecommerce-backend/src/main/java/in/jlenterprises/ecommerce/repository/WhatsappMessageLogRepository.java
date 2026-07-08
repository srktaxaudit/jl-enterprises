package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;
import in.jlenterprises.ecommerce.entity.WhatsappMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
