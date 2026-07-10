package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

/** One WhatsApp message to one recipient within a campaign — the per-message delivery log. */
@Entity
@Table(name = "whatsapp_message_logs",
        indexes = {
                @Index(name = "idx_wa_log_campaign", columnList = "campaign_id"),
                @Index(name = "idx_wa_log_provider_msg", columnList = "provider_message_id")
        })
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class WhatsappMessageLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_wa_log_campaign"))
    private WhatsappCampaign campaign;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "recipient_name", length = 160)
    private String recipientName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "rendered_body", nullable = false, length = 2000)
    private String renderedBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_status", nullable = false, length = 20)
    private WhatsappMessageStatus messageStatus = WhatsappMessageStatus.QUEUED;

    /** WhatsApp message id (real when live; a synthetic demo-<uuid> in demo mode). */
    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    @Column(name = "error", length = 500)
    private String error;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "sent_at")
    private Instant sentAt;

    /** Set from delivery-status webhooks (Phase 2). */
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;
}
