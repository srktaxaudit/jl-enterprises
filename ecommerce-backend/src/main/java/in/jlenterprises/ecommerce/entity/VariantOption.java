package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** A single name/value option pair on a variant, e.g. name="Color", value="White". */
@Entity
@Table(name = "variant_options", indexes = @Index(name = "idx_option_variant", columnList = "variant_id"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class VariantOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_option_variant"))
    private ProductVariant variant;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "value", nullable = false, length = 120)
    private String value;
}
