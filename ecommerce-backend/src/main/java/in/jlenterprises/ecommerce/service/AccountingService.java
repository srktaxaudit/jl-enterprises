package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.VoucherType;
import in.jlenterprises.ecommerce.dto.accounting.FinancialStatementDto;
import in.jlenterprises.ecommerce.dto.accounting.JournalEntryDto;
import in.jlenterprises.ecommerce.dto.accounting.LedgerAccountDto;
import in.jlenterprises.ecommerce.dto.accounting.LedgerStatementDto;
import in.jlenterprises.ecommerce.dto.accounting.TrialBalanceDto;
import in.jlenterprises.ecommerce.request.accounting.JournalEntryRequest;
import in.jlenterprises.ecommerce.request.accounting.LedgerAccountRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Double-entry accounting: chart of accounts, journals, ledgers and financial reports. */
public interface AccountingService {

    // ── Chart of accounts ──
    List<LedgerAccountDto> listAccounts(boolean includeInactive);
    LedgerAccountDto getAccount(UUID id);
    LedgerAccountDto createAccount(LedgerAccountRequest request);
    LedgerAccountDto updateAccount(UUID id, LedgerAccountRequest request);
    LedgerAccountDto setBlocked(UUID id, boolean blocked);
    LedgerAccountDto setActive(UUID id, boolean active);
    void deleteAccount(UUID id);

    // ── Journals ──
    Page<JournalEntryDto> listJournals(LocalDate from, LocalDate to, VoucherType type, Pageable pageable);
    JournalEntryDto getJournal(UUID id);
    JournalEntryDto postJournal(JournalEntryRequest request);

    // ── Reports ──
    LedgerStatementDto ledgerStatement(UUID accountId, LocalDate from, LocalDate to);
    TrialBalanceDto trialBalance(LocalDate asOf);
    FinancialStatementDto profitAndLoss(LocalDate from, LocalDate to);
    FinancialStatementDto balanceSheet(LocalDate asOf);

    // ── Integration ──
    /** Best-effort: post a Sales voucher for a paid order (idempotent). Never throws. */
    void postSaleForOrder(UUID orderId);

    /** Best-effort: reverse a previously posted Sales voucher for a returned/refunded order by
        posting a Credit Note (idempotent — only if a Sale exists and no reversal yet). Never throws. */
    void reverseSaleForOrder(UUID orderId);

    /** Net balance of an account (opening + all movements). +ve = debit balance, -ve = credit. */
    BigDecimal netBalance(UUID accountId);

    /** Seed the default chart of accounts if none exist (called on startup). */
    void ensureDefaultAccounts();

    /** A single posting instruction used when building a journal from a document. */
    record Posting(UUID accountId, BigDecimal debit, BigDecimal credit) {}

    /**
     * Post a balanced journal built from the given postings (used by the documents module).
     * Returns the created journal entry id. Throws if the postings are unbalanced.
     */
    UUID postingJournal(VoucherType type, LocalDate date, String reference, UUID referenceId,
                        String narration, List<Posting> postings);
}
