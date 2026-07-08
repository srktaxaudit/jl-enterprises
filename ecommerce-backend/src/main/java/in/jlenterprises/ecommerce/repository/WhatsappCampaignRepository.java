package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.WhatsappCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WhatsappCampaignRepository extends JpaRepository<WhatsappCampaign, UUID> {
    Page<WhatsappCampaign> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
