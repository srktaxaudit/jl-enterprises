package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.WhatsappTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsappTemplateRepository extends JpaRepository<WhatsappTemplate, UUID> {
    List<WhatsappTemplate> findAllByOrderByCreatedAtDesc();
    List<WhatsappTemplate> findByActiveTrueOrderByCreatedAtDesc();
    Optional<WhatsappTemplate> findFirstByMetaTemplateNameAndLanguage(String metaTemplateName, String language);
}
