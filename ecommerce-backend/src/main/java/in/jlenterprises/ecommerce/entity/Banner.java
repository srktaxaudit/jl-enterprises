package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/** A promotional banner shown on the storefront. */
@Entity
@Table(name = "banners")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Banner extends BaseEntity {

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    /** Placement slot, e.g. HERO, SIDEBAR, FOOTER. */
    @Column(name = "position", length = 40)
    private String position;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
