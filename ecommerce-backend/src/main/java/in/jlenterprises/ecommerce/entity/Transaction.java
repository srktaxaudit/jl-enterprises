package in.jlenterprises.ecommerce.entity;

import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.constant.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;

/** An individual charge or refund attempt against a payment. */
@Entity
@Table(name = "transactions", indexes = @Index(name = "idx_txn_payment", columnList = "payment_id"))
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
public class Transaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_txn_payment"))
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_status", nullable = false, length = 20)
    private PaymentStatus transactionStatus = PaymentStatus.PENDING;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "provider_reference", length = 160)
    private String providerReference;

    @Column(name = "raw_response", columnDefinition = "text")
    private String rawResponse;

    @Column(name = "processed_at")
    private Instant processedAt;
}
