package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A sellable product. Carries pricing, SEO fields, denormalised rating/sales
 * counters (kept fresh by services) and owns its images and variants.
 */
@Entity
@Table(name = "products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_slug", columnNames = "slug"),
                @UniqueConstraint(name = "uk_product_sku", columnNames = "sku")
        },
        indexes = {
                @Index(name = "idx_product_category", columnList = "category_id"),
                @Index(name = "idx_product_brand", columnList = "brand_id"),
                @Index(name = "idx_product_featured", columnList = "featured")
        })
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", nullable = false, length = 220)
    private String slug;

    @Column(name = "sku", nullable = false, length = 80)
    private String sku;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_product_category"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_product_brand"))
    private Brand brand;

    // ── Pricing ──
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Struck-through "compare at" / MRP price. */
    @Column(name = "compare_price", precision = 12, scale = 2)
    private BigDecimal comparePrice;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "featured", nullable = false)
    private boolean featured = false;

    // ── SEO ──
    @Column(name = "meta_title", length = 200)
    private String metaTitle;

    @Column(name = "meta_description", length = 300)
    private String metaDescription;

    /** Structured specs entered as "Key: Value" lines by the admin; shown as a table on the product page. */
    @Column(name = "specifications", columnDefinition = "text")
    private String specifications;

    // ── EMI (manually set per product by the admin — NO auto-calculation) ──
    /** columnDefinition default keeps ddl-auto's ALTER safe on the existing
        products table on Postgres (the NOT-NULL-with-existing-rows gotcha). */
    @Column(name = "emi_available", nullable = false, columnDefinition = "boolean not null default false")
    private boolean emiAvailable = false;

    @Column(name = "emi_months")
    private Integer emiMonths;

    @Column(name = "emi_amount", precision = 12, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "emi_down_payment", precision = 12, scale = 2)
    private BigDecimal emiDownPayment;

    @Column(name = "emi_processing_fee", precision = 12, scale = 2)
    private BigDecimal emiProcessingFee;

    @Column(name = "emi_note", columnDefinition = "text")
    private String emiNote;

    // ── Denormalised metrics ──
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    @Column(name = "sales_count", nullable = false)
    private long salesCount = 0;

    /** Available stock (quantity − reserved) for the detail view. Not persisted —
        the service sets it from the Inventory record when building ProductDetailDto. */
    @jakarta.persistence.Transient
    private Integer availableStock;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductVariant> variants = new ArrayList<>();
}
