package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.dto.admin.BillingRowDto;
import in.jlenterprises.ecommerce.dto.admin.BillingSummaryDto;
import in.jlenterprises.ecommerce.dto.order.InvoiceDto;
import in.jlenterprises.ecommerce.dto.order.OrderPaymentDto;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.BillingService;
import in.jlenterprises.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/billing")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin — Billing", description = "Invoices, payments ledger, revenue & GST (admin only)")
public class AdminBillingController {

    private final BillingService billingService;
    private final PaymentService paymentService;

    public AdminBillingController(BillingService billingService, PaymentService paymentService) {
        this.billingService = billingService;
        this.paymentService = paymentService;
    }

    @GetMapping("/invoices")
    @Operation(summary = "List invoices / payments (filterable ledger)")
    public ApiResponse<PageResponse<BillingRowDto>> invoices(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @PageableDefault(size = 25, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Instant toI = parse(to, Instant.now());
        Instant fromI = parse(from, toI.minus(Duration.ofDays(365)));
        return ApiResponse.success(PageResponse.of(
                billingService.listInvoices(status, method, blankToNull(q), fromI, toI, pageable)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Revenue & GST summary for a date range")
    public ApiResponse<BillingSummaryDto> summary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        Instant toI = parse(to, Instant.now());
        Instant fromI = parse(from, toI.minus(Duration.ofDays(365)));
        return ApiResponse.success(billingService.summary(fromI, toI));
    }

    @GetMapping("/invoices/{orderId}")
    @Operation(summary = "Full GST invoice for an order")
    public ApiResponse<InvoiceDto> invoice(@PathVariable UUID orderId) {
        return ApiResponse.success(billingService.adminInvoice(orderId));
    }

    @PostMapping("/orders/{orderId}/mark-paid")
    @Operation(summary = "Mark an order's payment as paid (COD collected / offline payment)")
    public ApiResponse<OrderPaymentDto> markPaid(@PathVariable UUID orderId) {
        return ApiResponse.success("Payment marked as paid", billingService.markPaid(orderId));
    }

    @PostMapping("/orders/{orderId}/refund")
    @Operation(summary = "Refund an order's payment")
    public ApiResponse<OrderPaymentDto> refund(@PathVariable UUID orderId) {
        return ApiResponse.success("Refund issued", paymentService.refund(orderId));
    }

    private static Instant parse(String iso, Instant fallback) {
        if (iso == null || iso.isBlank()) return fallback;
        try { return Instant.parse(iso.trim()); } catch (Exception e) { return fallback; }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
