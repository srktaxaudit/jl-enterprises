package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** A product brand / manufacturer. */
@Entity
@Table(name = "brands", uniqueConstraints = @UniqueConstraint(name = "uk_brand_slug", columnNames = "slug"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Brand extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "slug", nullable = false, length = 140)
    private String slug;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "description", columnDefinition = "text")
    private String description;
}
