package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

/**
 * One WhatsApp chat thread with a phone number (Phase 5 Inbox). Free-text replies are
 * allowed only within 24h of {@code lastInboundAt} — Meta's customer-service window.
 */
@Entity
@Table(name = "whatsapp_conversations",
        uniqueConstraints = @UniqueConstraint(name = "uk_wa_conv_phone", columnNames = "phone"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class WhatsappConversation extends BaseEntity {

    /** Normalized digits-only international number (as Meta reports it). */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /** WhatsApp profile name from the webhook contact block (may lag reality). */
    @Column(name = "contact_name", length = 160)
    private String contactName;

    /** Matched store customer, when the phone maps to a user (best-effort). */
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    /** Last customer message — anchors the 24h free-text reply window. */
    @Column(name = "last_inbound_at")
    private Instant lastInboundAt;

    @Column(name = "last_preview", length = 200)
    private String lastPreview;

    @Column(name = "unread_count", nullable = false)
    private int unreadCount = 0;
}
