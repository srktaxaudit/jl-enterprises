package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.constant.DocumentStatus;
import in.jlenterprises.ecommerce.constant.DocumentType;
import in.jlenterprises.ecommerce.dto.accounting.DocumentDto;
import in.jlenterprises.ecommerce.dto.accounting.DocumentSummaryDto;
import in.jlenterprises.ecommerce.request.accounting.DocumentRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.DocumentService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/documents")
@PreAuthorize("hasAnyRole('ACCOUNTANT','ADMIN','SUPER_ADMIN')")
@Tag(name = "Admin — Documents", description = "Invoices, bills, estimates, credit/debit notes (posts to GL)")
public class DocumentController {

    private final DocumentService documents;

    public DocumentController(DocumentService documents) {
        this.documents = documents;
    }

    @GetMapping
    @Operation(summary = "List documents / registers")
    public ApiResponse<PageResponse<DocumentSummaryDto>> list(
            @RequestParam(required = false) DocumentType type,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 25, sort = "documentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(documents.list(type, status, from, to, q, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentDto> get(@PathVariable UUID id) {
        return ApiResponse.success(documents.get(id));
    }

    @PostMapping
    @Operation(summary = "Create a document (draft)")
    public ApiResponse<DocumentDto> create(@Valid @RequestBody DocumentRequest request) {
        return ApiResponse.success("Document saved", documents.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DocumentDto> update(@PathVariable UUID id, @Valid @RequestBody DocumentRequest request) {
        return ApiResponse.success("Document updated", documents.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        documents.delete(id);
        return ApiResponse.success("Document deleted", null);
    }

    @PostMapping("/{id}/post")
    @Operation(summary = "Post a document to the ledgers")
    public ApiResponse<DocumentDto> post(@PathVariable UUID id) {
        return ApiResponse.success("Document posted", documents.post(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel / void a document")
    public ApiResponse<DocumentDto> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Document cancelled", documents.cancel(id));
    }

    @PostMapping("/{id}/convert")
    @Operation(summary = "Convert an estimate into a sales invoice")
    public ApiResponse<DocumentDto> convert(@PathVariable UUID id) {
        return ApiResponse.success("Estimate converted to invoice", documents.convertEstimate(id));
    }
}
