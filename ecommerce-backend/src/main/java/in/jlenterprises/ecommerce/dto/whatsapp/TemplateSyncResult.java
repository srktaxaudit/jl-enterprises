package in.jlenterprises.ecommerce.dto.whatsapp;

/** Outcome of syncing message templates from Meta. */
public record TemplateSyncResult(
        int fetched,
        int imported,
        int updated,
        int skipped,
        String message
) {}
