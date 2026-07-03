package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Append-only audit trail of significant actions (who did what, to which entity).
 * Not soft-deletable and not filtered — audit records are immutable history, so
 * this entity deliberately omits the {@code @SQLRestriction} the others carry.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_actor", columnList = "actor"),
        @Index(name = "idx_audit_entity", columnList = "entity"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditLog extends BaseEntity {

    /** Email / username of the actor, or "system". */
    @Column(name = "actor", length = 160)
    private String actor;

    @Column(name = "action", nullable = false, length = 80)
    private String action;

    @Column(name = "entity", length = 80)
    private String entity;

    @Column(name = "entity_id", length = 80)
    private String entityId;

    @Column(name = "detail", columnDefinition = "text")
    private String detail;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;
}
