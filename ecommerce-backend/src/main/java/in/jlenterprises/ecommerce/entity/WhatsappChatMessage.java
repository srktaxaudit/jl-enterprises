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

/** One message (either direction) inside an Inbox conversation. */
@Entity
@Table(name = "whatsapp_chat_messages",
        indexes = {
                @Index(name = "idx_wa_chat_conv", columnList = "conversation_id"),
                @Index(name = "idx_wa_chat_provider", columnList = "provider_message_id")
        })
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class WhatsappChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_wa_chat_conv"))
    private WhatsappConversation conversation;

    /** IN = from the customer, OUT = sent by staff. */
    @Column(name = "direction", nullable = false, length = 3)
    private String direction;

    /** Meta message type (text/image/document/audio/…); non-text bodies get a placeholder. */
    @Column(name = "message_type", nullable = false, length = 20)
    private String messageType = "text";

    @Column(name = "body", nullable = false, length = 2000)
    private String body;

    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    /** Delivery state — meaningful for OUT; IN rows stay DELIVERED. */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_status", nullable = false, length = 20)
    private WhatsappMessageStatus messageStatus = WhatsappMessageStatus.SENT;

    @Column(name = "error", length = 500)
    private String error;

    /** When the message actually happened (webhook timestamp for IN, send time for OUT). */
    @Column(name = "event_at", nullable = false)
    private Instant eventAt;
}
