package in.jlenterprises.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A monotonic counter keyed by an arbitrary string (e.g. "V:SALES:2026-27").
 * Allocated under a pessimistic row lock so concurrent posts never collide, and
 * because the value only ever advances, deleting a voucher/document can never
 * cause a number to be reused. Deliberately NOT a {@code BaseEntity} — it's an
 * internal counter with no soft-delete / audit semantics.
 */
@Entity
@Table(name = "number_sequences")
public class NumberSequence {

    @Id
    @Column(name = "seq_key", length = 80, nullable = false)
    private String seqKey;

    @Column(name = "next_value", nullable = false)
    private long nextValue;

    public NumberSequence() {}

    public NumberSequence(String seqKey, long nextValue) {
        this.seqKey = seqKey;
        this.nextValue = nextValue;
    }

    public String getSeqKey() { return seqKey; }
    public void setSeqKey(String seqKey) { this.seqKey = seqKey; }

    public long getNextValue() { return nextValue; }
    public void setNextValue(long nextValue) { this.nextValue = nextValue; }
}
