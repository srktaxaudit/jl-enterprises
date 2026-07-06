package in.jlenterprises.ecommerce.constant;

/** The kind of transaction a journal entry represents (voucher classification). */
public enum VoucherType {
    SALES,
    PURCHASE,
    RECEIPT,
    PAYMENT,
    JOURNAL,
    CONTRA,
    CREDIT_NOTE,
    DEBIT_NOTE
}
