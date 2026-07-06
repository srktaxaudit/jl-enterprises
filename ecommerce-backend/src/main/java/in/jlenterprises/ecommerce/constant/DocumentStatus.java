package in.jlenterprises.ecommerce.constant;

/** Lifecycle of an accounting document. */
public enum DocumentStatus {
    DRAFT,       // saved, not yet in the books
    POSTED,      // journal created; affects the ledgers
    CONVERTED,   // estimate turned into an invoice
    CANCELLED    // voided (journal reversed/removed)
}
