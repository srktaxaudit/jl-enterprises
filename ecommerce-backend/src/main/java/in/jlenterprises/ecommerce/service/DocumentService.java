package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.DocumentStatus;
import in.jlenterprises.ecommerce.constant.DocumentType;
import in.jlenterprises.ecommerce.dto.accounting.DocumentDto;
import in.jlenterprises.ecommerce.dto.accounting.DocumentSummaryDto;
import in.jlenterprises.ecommerce.request.accounting.DocumentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/** Sales invoices, purchase bills, estimates and credit/debit notes — each posts to the GL. */
public interface DocumentService {

    Page<DocumentSummaryDto> list(DocumentType type, DocumentStatus status,
                                  LocalDate from, LocalDate to, String q, Pageable pageable);

    DocumentDto get(UUID id);

    DocumentDto create(DocumentRequest request);

    DocumentDto update(UUID id, DocumentRequest request);

    void delete(UUID id);

    /** Draft → Posted: creates the double-entry journal. */
    DocumentDto post(UUID id);

    /** Reverse/void a document (removes its journal from the ledgers). */
    DocumentDto cancel(UUID id);

    /** Estimate → a new draft Sales Invoice (returns the new invoice). */
    DocumentDto convertEstimate(UUID id);
}
