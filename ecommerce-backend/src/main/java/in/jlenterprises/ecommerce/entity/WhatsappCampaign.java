package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.WhatsappAudienceType;
import in.jlenterprises.ecommerce.constant.WhatsappCampaignStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/** A WhatsApp marketing campaign: a message + audience + send status + roll-up counts. */
@Entity
@Table(name = "whatsapp_campaigns")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class WhatsappCampaign extends BaseEntity {

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    /** Optional saved template this campaign was composed from. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_wa_campaign_template"))
    private WhatsappTemplate template;

    /** The message body actually used (with {{name}} tokens), snapshotted at compose time. */
    @Column(name = "body_text", nullable = false, length = 2000)
    private String bodyText;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 30)
    private WhatsappAudienceType audienceType = WhatsappAudienceType.ALL_OPTED_IN;

    @Column(name = "city_filter", length = 80)
    private String cityFilter;

    /** Comma-separated user ids when audienceType = MANUAL (hand-picked recipients). */
    @Column(name = "manual_recipient_ids", columnDefinition = "text")
    private String manualRecipientIds;

    /** Set on the per-event container campaigns that collect automation sends (Phase 4). */
    @Column(name = "automation_event", length = 40)
    private String automationEvent;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_status", nullable = false, length = 20)
    private WhatsappCampaignStatus campaignStatus = WhatsappCampaignStatus.DRAFT;

    @Column(name = "demo_mode", nullable = false)
    private boolean demoMode = true;

    /** Reserved for Phase 2 scheduling. */
    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "total_recipients", nullable = false)
    private int totalRecipients = 0;

    @Column(name = "sent_count", nullable = false)
    private int sentCount = 0;

    @Column(name = "delivered_count", nullable = false)
    private int deliveredCount = 0;

    @Column(name = "read_count", nullable = false)
    private int readCount = 0;

    @Column(name = "failed_count", nullable = false)
    private int failedCount = 0;
}
