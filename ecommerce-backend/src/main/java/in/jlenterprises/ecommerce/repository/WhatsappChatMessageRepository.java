package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.WhatsappChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsappChatMessageRepository extends JpaRepository<WhatsappChatMessage, UUID> {
    /** Latest 200, newest first — the service reverses them for display. */
    List<WhatsappChatMessage> findTop200ByConversation_IdOrderByEventAtDesc(UUID conversationId);
    boolean existsByProviderMessageId(String providerMessageId);
    Optional<WhatsappChatMessage> findByProviderMessageId(String providerMessageId);
}
