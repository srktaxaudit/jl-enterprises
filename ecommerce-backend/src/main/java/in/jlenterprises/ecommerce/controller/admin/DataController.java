package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.constant.DocumentType;
import in.jlenterprises.ecommerce.dto.accounting.BackupDto;
import in.jlenterprises.ecommerce.dto.accounting.ImportResultDto;
import in.jlenterprises.ecommerce.dto.migration.MigrationResult;
import in.jlenterprises.ecommerce.request.migration.VyaparPackage;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.service.ImportExportService;
import in.jlenterprises.ecommerce.service.VyaparImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/data")
@PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin — Import/Export", description = "Export the books (CSV / Tally XML / JSON) and import ledgers")
public class DataController {

    private static final LocalDate ALL_FROM = LocalDate.of(2000, 1, 1);

    private final ImportExportService io;
    private final VyaparImportService vyapar;

    public DataController(ImportExportService io, VyaparImportService vyapar) {
        this.io = io;
        this.vyapar = vyapar;
    }

    @GetMapping("/export/accounts")
    @Operation(summary = "Chart of accounts as CSV")
    public ApiResponse<String> accounts() {
        return ApiResponse.success(io.accountsCsv());
    }

    @GetMapping("/export/journals")
    @Operation(summary = "Journal/day-book lines as CSV")
    public ApiResponse<String> journals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(io.journalsCsv(from != null ? from : ALL_FROM, to != null ? to : LocalDate.now()));
    }

    @GetMapping("/export/documents")
    @Operation(summary = "Documents register as CSV")
    public ApiResponse<String> documentsCsv(
            @RequestParam(required = false) DocumentType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(io.documentsCsv(type, from != null ? from : ALL_FROM, to != null ? to : LocalDate.now()));
    }

    @GetMapping("/export/trial-balance")
    @Operation(summary = "Trial balance as CSV")
    public ApiResponse<String> trialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ApiResponse.success(io.trialBalanceCsv(asOf != null ? asOf : LocalDate.now()));
    }

    @GetMapping("/export/tally")
    @Operation(summary = "Tally-compatible XML (masters + vouchers)")
    public ApiResponse<String> tally(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(io.tallyXml(from != null ? from : ALL_FROM, to != null ? to : LocalDate.now()));
    }

    @GetMapping("/backup")
    @Operation(summary = "Full JSON backup of the books")
    public ApiResponse<BackupDto> backup() {
        return ApiResponse.success(io.backup());
    }

    @PostMapping("/import/accounts")
    @Operation(summary = "Import ledger accounts from CSV")
    public ApiResponse<ImportResultDto> importAccounts(@RequestBody Map<String, String> body) {
        return ApiResponse.success("Import complete", io.importAccounts(body.get("csv")));
    }

    // ── Vyapar migration (opening-balance method) ──────────────────────────

    @PostMapping("/import/vyapar")
    @Operation(summary = "Migrate a Vyapar backup (products, party ledgers, contacts, opening balances). "
            + "dryRun=true reconciles without writing.")
    public ApiResponse<MigrationResult> importVyapar(
            @RequestParam(defaultValue = "false") boolean dryRun,
            @RequestBody VyaparPackage pkg) {
        MigrationResult result = vyapar.run(pkg, dryRun);
        return ApiResponse.success(dryRun ? "Dry-run complete" : "Vyapar import complete", result);
    }

    @PostMapping("/import/vyapar/rollback")
    @Operation(summary = "Undo the Vyapar import — remove all VYP- products/ledgers/contacts and reset opening balances")
    public ApiResponse<MigrationResult> rollbackVyapar() {
        return ApiResponse.success("Vyapar import rolled back", vyapar.rollback());
    }
}
