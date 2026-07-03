package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/** A fine-grained permission (e.g. {@code PRODUCT_WRITE}) grantable to roles. */
@Entity
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(name = "uk_permission_name", columnNames = "name"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "description", length = 200)
    private String description;
}
