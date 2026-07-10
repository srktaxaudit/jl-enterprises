package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.WhatsappAutomationEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** Maps one store event to a WhatsApp template; disabled rules (or missing templates) do nothing. */
@Entity
@Table(name = "whatsapp_automation_rules",
        uniqueConstraints = @UniqueConstraint(name = "uk_wa_auto_event", columnNames = "event"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class WhatsappAutomationRule extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, length = 40)
    private WhatsappAutomationEvent event;

    /** Template to send. Live sends require its Meta template name to be approved. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_wa_auto_template"))
    private WhatsappTemplate template;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = false;
}
