package in.jlenterprises.ecommerce.request.admin;

import java.math.BigDecimal;

/**
 * One row of a bulk product update (matched to an existing product by SKU).
 * Null fields are left unchanged, so a CSV can update just price, just stock, etc.
 */
public record ProductBulkRow(
        String sku,
        BigDecimal price,
        BigDecimal comparePrice,
        Integer stock,
        Integer reorderLevel,
        Boolean featured,
        Boolean active
) {}
