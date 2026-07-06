package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.constant.VoucherType;
import in.jlenterprises.ecommerce.dto.accounting.FinancialStatementDto;
import in.jlenterprises.ecommerce.dto.accounting.JournalEntryDto;
import in.jlenterprises.ecommerce.dto.accounting.LedgerAccountDto;
import in.jlenterprises.ecommerce.dto.accounting.LedgerStatementDto;
import in.jlenterprises.ecommerce.dto.accounting.TrialBalanceDto;
import in.jlenterprises.ecommerce.request.accounting.JournalEntryRequest;
import in.jlenterprises.ecommerce.request.accounting.LedgerAccountRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.AccountingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/accounting")
@PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin — Accounting", description = "Double-entry books: chart of accounts, journals, ledgers, reports")
public class AccountingController {

    private final AccountingService accounting;

    public AccountingController(AccountingService accounting) {
        this.accounting = accounting;
    }

    // ── Chart of accounts ──
    @GetMapping("/accounts")
    @Operation(summary = "List ledger accounts (chart of accounts)")
    public ApiResponse<List<LedgerAccountDto>> accounts(@RequestParam(defaultValue = "true") boolean includeInactive) {
        return ApiResponse.success(accounting.listAccounts(includeInactive));
    }

    @GetMapping("/accounts/{id}")
    public ApiResponse<LedgerAccountDto> account(@PathVariable UUID id) {
        return ApiResponse.success(accounting.getAccount(id));
    }

    @PostMapping("/accounts")
    @Operation(summary = "Create a ledger account")
    public ApiResponse<LedgerAccountDto> createAccount(@Valid @RequestBody LedgerAccountRequest request) {
        return ApiResponse.success("Account created", accounting.createAccount(request));
    }

    @PutMapping("/accounts/{id}")
    public ApiResponse<LedgerAccountDto> updateAccount(@PathVariable UUID id, @Valid @RequestBody LedgerAccountRequest request) {
        return ApiResponse.success("Account updated", accounting.updateAccount(id, request));
    }

    @PatchMapping("/accounts/{id}/blocked")
    @Operation(summary = "Block / unblock an account for posting")
    public ApiResponse<LedgerAccountDto> setBlocked(@PathVariable UUID id, @RequestParam boolean blocked) {
        return ApiResponse.success(accounting.setBlocked(id, blocked));
    }

    @PatchMapping("/accounts/{id}/active")
    @Operation(summary = "Activate / deactivate an account")
    public ApiResponse<LedgerAccountDto> setActive(@PathVariable UUID id, @RequestParam boolean active) {
        return ApiResponse.success(accounting.setActive(id, active));
    }

    @DeleteMapping("/accounts/{id}")
    public ApiResponse<Void> deleteAccount(@PathVariable UUID id) {
        accounting.deleteAccount(id);
        return ApiResponse.success("Account deleted", null);
    }

    // ── Journals ──
    @GetMapping("/journals")
    @Operation(summary = "List journal vouchers")
    public ApiResponse<PageResponse<JournalEntryDto>> journals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) VoucherType type,
            @PageableDefault(size = 25, sort = "entryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(accounting.listJournals(from, to, type, pageable)));
    }

    @GetMapping("/journals/{id}")
    public ApiResponse<JournalEntryDto> journal(@PathVariable UUID id) {
        return ApiResponse.success(accounting.getJournal(id));
    }

    @PostMapping("/journals")
    @Operation(summary = "Post a balanced journal voucher")
    public ApiResponse<JournalEntryDto> postJournal(@Valid @RequestBody JournalEntryRequest request) {
        return ApiResponse.success("Voucher posted", accounting.postJournal(request));
    }

    // ── Reports ──
    @GetMapping("/ledger/{accountId}")
    @Operation(summary = "Ledger statement for an account")
    public ApiResponse<LedgerStatementDto> ledger(
            @PathVariable UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate t = to != null ? to : LocalDate.now();
        LocalDate f = from != null ? from : t.minusYears(1);
        return ApiResponse.success(accounting.ledgerStatement(accountId, f, t));
    }

    @GetMapping("/reports/trial-balance")
    public ApiResponse<TrialBalanceDto> trialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ApiResponse.success(accounting.trialBalance(asOf != null ? asOf : LocalDate.now()));
    }

    @GetMapping("/reports/pnl")
    public ApiResponse<FinancialStatementDto> profitAndLoss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate t = to != null ? to : LocalDate.now();
        LocalDate f = from != null ? from : t.withDayOfYear(1);
        return ApiResponse.success(accounting.profitAndLoss(f, t));
    }

    @GetMapping("/reports/balance-sheet")
    public ApiResponse<FinancialStatementDto> balanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ApiResponse.success(accounting.balanceSheet(asOf != null ? asOf : LocalDate.now()));
    }
}
