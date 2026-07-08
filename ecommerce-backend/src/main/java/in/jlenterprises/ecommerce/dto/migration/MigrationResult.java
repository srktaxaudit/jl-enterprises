package in.jlenterprises.ecommerce.dto.migration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outcome of a Vyapar import (or dry-run). The counts + the opening trial-balance
 * figures let the admin reconcile against Vyapar's own reports before committing.
 */
public record MigrationResult(
        boolean dryRun,
        // ── catalogue ──
        int productsCreated,
        int productsUpdated,
        int productsSkipped,
        // ── party ledgers ──
        int partyLedgers,
        BigDecimal receivablesTotal,
        BigDecimal payablesTotal,
        // ── CRM contacts ──
        int contactsCreated,
        int contactsSkipped,
        // ── opening trial balance ──
        BigDecimal cash,
        BigDecimal bank,
        BigDecimal stockValue,
        BigDecimal capitalBalancing,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        boolean trialBalanceOk,
        List<String> warnings
) {}
