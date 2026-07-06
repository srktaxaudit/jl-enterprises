package in.jlenterprises.ecommerce.dto.accounting;

import java.time.LocalDate;
import java.util.List;

/** A point-in-time snapshot of the books for safekeeping/export. */
public record BackupDto(
        LocalDate generatedOn,
        List<LedgerAccountDto> accounts,
        List<JournalEntryDto> journals,
        List<DocumentSummaryDto> documents
) {}
