package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.WhatsappConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsappConversationRepository extends JpaRepository<WhatsappConversation, UUID> {
    Optional<WhatsappConversation> findByPhone(String phone);
    Page<WhatsappConversation> findAllByOrderByLastMessageAtDesc(Pageable pageable);
    long countByUnreadCountGreaterThan(int threshold);
}
