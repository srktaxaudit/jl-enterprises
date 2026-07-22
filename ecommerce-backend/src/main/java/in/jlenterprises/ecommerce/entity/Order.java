package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.OrderStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A customer order with monetary breakdown, status and embedded address snapshots. */
@Entity
@Table(name = "orders",
        uniqueConstraints = @UniqueConstraint(name = "uk_order_number", columnNames = "order_number"),
        indexes = {
                @Index(name = "idx_order_user", columnList = "user_id"),
                @Index(name = "idx_order_status", columnList = "order_status"),
                // Admin date filters, revenue/billing ranges and the abandoned-order
                // sweeper all scan by placed_at.
                @Index(name = "idx_order_placed_at", columnList = "placed_at")
        })
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Order extends BaseEntity {

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_order_user"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    // ── Money ──
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "tax_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxTotal = BigDecimal.ZERO;

    @Column(name = "shipping_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "INR";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", foreignKey = @jakarta.persistence.ForeignKey(name = "fk_order_coupon"))
    private Coupon coupon;

    /** Trade-in value applied from an approved exchange request (0/none).
        Nullable at the DB level so ddl-auto=update can add the column to the
        existing orders table without a NOT NULL rewrite; new orders always set it. */
    @Column(name = "exchange_value", precision = 12, scale = 2)
    private BigDecimal exchangeValue = BigDecimal.ZERO;

    /** The exchange request consumed by this order (null if none). */
    @Column(name = "exchange_request_id")
    private java.util.UUID exchangeRequestId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullName",   column = @Column(name = "ship_full_name", length = 120)),
            @AttributeOverride(name = "phone",      column = @Column(name = "ship_phone", length = 20)),
            @AttributeOverride(name = "line1",      column = @Column(name = "ship_line1", length = 200)),
            @AttributeOverride(name = "line2",      column = @Column(name = "ship_line2", length = 200)),
            @AttributeOverride(name = "city",       column = @Column(name = "ship_city", length = 80)),
            @AttributeOverride(name = "state",      column = @Column(name = "ship_state", length = 80)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "ship_postal_code", length = 20)),
            @AttributeOverride(name = "country",    column = @Column(name = "ship_country", length = 80))
    })
    private AddressSnapshot shippingAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullName",   column = @Column(name = "bill_full_name", length = 120)),
            @AttributeOverride(name = "phone",      column = @Column(name = "bill_phone", length = 20)),
            @AttributeOverride(name = "line1",      column = @Column(name = "bill_line1", length = 200)),
            @AttributeOverride(name = "line2",      column = @Column(name = "bill_line2", length = 200)),
            @AttributeOverride(name = "city",       column = @Column(name = "bill_city", length = 80)),
            @AttributeOverride(name = "state",      column = @Column(name = "bill_state", length = 80)),
            @AttributeOverride(name = "postalCode", column = @Column(name = "bill_postal_code", length = 20)),
            @AttributeOverride(name = "country",    column = @Column(name = "bill_country", length = 80))
    })
    private AddressSnapshot billingAddress;

    @Column(name = "placed_at")
    private Instant placedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    // ── Lifecycle (cancellation / return) ──
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 300)
    private String cancellationReason;

    @Column(name = "return_requested_at")
    private Instant returnRequestedAt;

    @Column(name = "return_reason", length = 300)
    private String returnReason;

    /** Staff-only internal notes (not shown to the customer). */
    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    /** Inverse side of {@link Payment#getOrder()} — the owning FK lives on payments. */
    @jakarta.persistence.OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private Payment payment;
}
