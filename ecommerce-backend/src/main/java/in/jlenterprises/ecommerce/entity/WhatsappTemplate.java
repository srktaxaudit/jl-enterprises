package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.WhatsappTemplateCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** A reusable WhatsApp message. When live, it must map to a Meta-approved template. */
@Entity
@Table(name = "whatsapp_templates")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class WhatsappTemplate extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** The exact template name approved in Meta (blank until submitted/approved). */
    @Column(name = "meta_template_name", length = 160)
    private String metaTemplateName;

    /** Meta review status when synced (APPROVED/PENDING/REJECTED); null for local-only templates. */
    @Column(name = "meta_status", length = 20)
    private String metaStatus;

    @Column(name = "language", nullable = false, length = 10)
    private String language = "en";

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private WhatsappTemplateCategory category = WhatsappTemplateCategory.MARKETING;

    /** Body with {{name}} personalization tokens. */
    @Column(name = "body_text", nullable = false, length = 2000)
    private String bodyText;

    /** NONE for now; TEXT/IMAGE/DOCUMENT come in Phase 2 (media). */
    @Column(name = "header_type", nullable = false, length = 20)
    private String headerType = "NONE";

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
