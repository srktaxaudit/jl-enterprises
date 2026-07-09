package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/** A persisted refresh token (rotation-friendly), tied to a user and device. */
@Entity
@Table(name = "refresh_tokens",
        uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token", columnNames = "token"),
        indexes = @Index(name = "idx_refresh_user", columnList = "user_id"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken extends BaseEntity {

    /** SHA-256 hash of the opaque token (hex). The raw value is never stored at rest. */
    @Column(name = "token", nullable = false, length = 200)
    private String token;

    /** The raw opaque token — only populated transiently when a token is issued/rotated,
        so the caller can hand it to the client. Never persisted. */
    @jakarta.persistence.Transient
    private String rawToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_refresh_user"))
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "remember_me", nullable = false)
    private boolean rememberMe = false;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    public boolean isActive() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }
}
