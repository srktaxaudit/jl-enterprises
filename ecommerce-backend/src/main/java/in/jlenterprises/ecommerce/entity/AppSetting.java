package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Simple key/value store for business settings (GSTIN, store hours, exchange
 * bonus, …). Keyed by string — deliberately does NOT extend {@link BaseEntity}
 * (no UUID/soft-delete needed for a config row).
 */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
public class AppSetting {

    @Id
    @Column(name = "setting_key", length = 120, nullable = false)
    private String key;

    /** Free-form value; may hold plain text or a JSON string. */
    @Column(name = "setting_value", columnDefinition = "text")
    private String value;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
