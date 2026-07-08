package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.NotificationType;
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

/** An in-app notification for a user. */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user", columnList = "user_id"),
        @Index(name = "idx_notification_read", columnList = "is_read")
})
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_notification_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type = NotificationType.SYSTEM;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "link", length = 500)
    private String link;

    // ── Optional context for admin notifications (which section/record it refers to) ──
    /** Human-readable section name, e.g. "Orders", "Service Bookings". */
    @Column(name = "section", length = 60)
    private String section;

    /** Id of the related record (order/booking/etc.) so the UI can deep-link. */
    @Column(name = "related_id")
    private UUID relatedId;

    /** Type of the related record, e.g. "ORDER", "SERVICE_BOOKING". */
    @Column(name = "related_type", length = 40)
    private String relatedType;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;
}
