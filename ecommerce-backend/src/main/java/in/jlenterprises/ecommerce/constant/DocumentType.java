package in.jlenterprises.ecommerce.constant;

/** Accounting/trade documents. Each (except ESTIMATE) posts a double-entry journal when posted. */
public enum DocumentType {
    SALES_INVOICE,
    PURCHASE_BILL,
    ESTIMATE,
    CREDIT_NOTE,   // sales return (reduces Sales + Output GST + Debtor)
    DEBIT_NOTE;    // purchase return (reduces Purchases + Input GST + Creditor)

    public boolean isSalesSide() {
        return this == SALES_INVOICE || this == ESTIMATE || this == CREDIT_NOTE;
    }
}
