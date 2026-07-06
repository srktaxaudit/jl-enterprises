package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.dto.accounting.AgingReportDto;
import in.jlenterprises.ecommerce.dto.accounting.CashFlowDto;
import in.jlenterprises.ecommerce.dto.accounting.EwayBillDto;
import in.jlenterprises.ecommerce.dto.accounting.GstReturnDto;
import in.jlenterprises.ecommerce.dto.accounting.Gstr3bDto;
import in.jlenterprises.ecommerce.dto.accounting.JournalEntryDto;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.AccountingService;
import in.jlenterprises.ecommerce.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/compliance")
@PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin — Compliance", description = "GST returns, e-way bill, cash flow, aging, day book")
public class ComplianceController {

    private final ComplianceService compliance;
    private final AccountingService accounting;

    public ComplianceController(ComplianceService compliance, AccountingService accounting) {
        this.compliance = compliance;
        this.accounting = accounting;
    }

    @GetMapping("/gstr1")
    @Operation(summary = "GSTR-1 (outward supplies) working data")
    public ApiResponse<GstReturnDto> gstr1(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] p = period(from, to);
        return ApiResponse.success(compliance.gstr1(p[0], p[1]));
    }

    @GetMapping("/gstr2")
    @Operation(summary = "GSTR-2 (inward supplies) working data")
    public ApiResponse<GstReturnDto> gstr2(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] p = period(from, to);
        return ApiResponse.success(compliance.gstr2(p[0], p[1]));
    }

    @GetMapping("/gstr3b")
    @Operation(summary = "GSTR-3B summary (net GST payable)")
    public ApiResponse<Gstr3bDto> gstr3b(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] p = period(from, to);
        return ApiResponse.success(compliance.gstr3b(p[0], p[1]));
    }

    @GetMapping("/cash-flow")
    @Operation(summary = "Cash & bank movement statement")
    public ApiResponse<CashFlowDto> cashFlow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate[] p = period(from, to);
        return ApiResponse.success(compliance.cashFlow(p[0], p[1]));
    }

    @GetMapping("/aging")
    @Operation(summary = "Outstanding receivables/payables aged by days")
    public ApiResponse<AgingReportDto> aging(
            @RequestParam(defaultValue = "RECEIVABLE") String kind,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ApiResponse.success(compliance.aging(kind, asOf != null ? asOf : LocalDate.now()));
    }

    @GetMapping("/eway-bill/{documentId}")
    @Operation(summary = "E-way bill working data for a document")
    public ApiResponse<EwayBillDto> ewayBill(@PathVariable UUID documentId) {
        return ApiResponse.success(compliance.ewayBill(documentId));
    }

    @GetMapping("/day-book")
    @Operation(summary = "Day book — all vouchers in a period (audit report)")
    public ApiResponse<PageResponse<JournalEntryDto>> dayBook(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50, sort = "entryDate", direction = Sort.Direction.DESC) Pageable pageable) {
        LocalDate[] p = period(from, to);
        return ApiResponse.success(PageResponse.of(accounting.listJournals(p[0], p[1], null, pageable)));
    }

    /** Default period = current month to date. */
    private LocalDate[] period(LocalDate from, LocalDate to) {
        LocalDate t = to != null ? to : LocalDate.now();
        LocalDate f = from != null ? from : t.withDayOfMonth(1);
        return new LocalDate[]{f, t};
    }
}
