package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.constant.WhatsappAutomationEvent;
import in.jlenterprises.ecommerce.entity.WhatsappAutomationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsappAutomationRuleRepository extends JpaRepository<WhatsappAutomationRule, UUID> {
    Optional<WhatsappAutomationRule> findByEvent(WhatsappAutomationEvent event);
}
