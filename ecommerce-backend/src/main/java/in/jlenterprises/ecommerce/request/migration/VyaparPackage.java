package in.jlenterprises.ecommerce.request.migration;

import java.math.BigDecimal;
import java.util.List;

/**
 * The offline-built migration package uploaded by the admin (produced from a
 * Vyapar {@code .vyp} SQLite backup). Carries the catalogue, party ledgers +
 * contact details, and the opening-balance figures. No transaction history —
 * this is the opening-balance migration method (see VyaparImportService).
 */
public record VyaparPackage(
        String source,
        String firm,
        Opening opening,
        List<Product> products,
        List<Party> parties
) {

    /** A catalogue item. {@code sku} is already VYP-prefixed by the transform. */
    public record Product(
            String sku,
            String name,
            BigDecimal price,
            BigDecimal comparePrice,
            Integer stock,
            Integer reorder,
            String description,
            String category
    ) {}

    /**
     * A Vyapar party. Becomes a ledger account (when it has an outstanding
     * balance) and, for CUSTOMER-type parties with a phone, a CRM contact.
     */
    public record Party(
            String code,          // "VYP-P-<name_id>"
            String name,
            String type,          // CUSTOMER | SUPPLIER
            BigDecimal openingBalance,
            String openingSide,   // DR (receivable) | CR (payable)
            String gstin,
            BigDecimal creditLimit,
            String phone,
            String email,
            String address,
            String state
    ) {}

    /** Opening figures as of the cutoff date. Cash/bank are owner-entered in the UI. */
    public record Opening(
            String asOf,
            BigDecimal cash,
            BigDecimal bank,
            BigDecimal stockValue
    ) {}
}
