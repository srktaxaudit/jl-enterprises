package in.jlenterprises.ecommerce.constant;

/**
 * Fundamental account classes for double-entry accounting. ASSET and EXPENSE
 * accounts carry a normal DEBIT balance; LIABILITY, EQUITY and INCOME carry a
 * normal CREDIT balance. INCOME/EXPENSE feed the P&amp;L; the rest feed the Balance Sheet.
 */
public enum AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    INCOME,
    EXPENSE;

    /** True if this account normally carries a debit balance (Assets, Expenses). */
    public boolean debitNormal() {
        return this == ASSET || this == EXPENSE;
    }

    /** True if this account belongs on the Profit &amp; Loss statement. */
    public boolean isProfitAndLoss() {
        return this == INCOME || this == EXPENSE;
    }
}
