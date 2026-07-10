package in.jlenterprises.ecommerce.dto.admin;

import java.util.List;

/** Outcome of a bulk product update: how many rows updated, and which were skipped and why. */
public record BulkUpdateResult(int updated, List<Skipped> skipped) {
    public record Skipped(String sku, String reason) {}
}
