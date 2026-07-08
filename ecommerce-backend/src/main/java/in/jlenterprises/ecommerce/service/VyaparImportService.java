package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.migration.MigrationResult;
import in.jlenterprises.ecommerce.request.migration.VyaparPackage;

/**
 * Opening-balance migration of a Vyapar backup into the store: catalogue + stock,
 * party ledgers with outstanding balances, customer contacts, and the opening
 * trial balance. Idempotent (keyed by VYP- prefixes) and reversible.
 */
public interface VyaparImportService {

    /** Import the package. {@code dryRun=true} computes counts + reconciliation without writing. */
    MigrationResult run(VyaparPackage pkg, boolean dryRun);

    /** Undo a previous import: remove all VYP- products/ledgers/contacts and zero the opening balances. */
    MigrationResult rollback();
}
