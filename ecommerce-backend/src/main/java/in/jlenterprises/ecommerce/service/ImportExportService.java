package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.DocumentType;
import in.jlenterprises.ecommerce.dto.accounting.BackupDto;
import in.jlenterprises.ecommerce.dto.accounting.ImportResultDto;

import java.time.LocalDate;

/** CSV / Tally-XML / JSON export of the books, and Chart-of-Accounts CSV import. */
public interface ImportExportService {

    String accountsCsv();

    String journalsCsv(LocalDate from, LocalDate to);

    String documentsCsv(DocumentType type, LocalDate from, LocalDate to);

    String trialBalanceCsv(LocalDate asOf);

    /** Tally-compatible XML: ledger masters + journal vouchers (best-effort format). */
    String tallyXml(LocalDate from, LocalDate to);

    /** Full JSON snapshot of the books. */
    BackupDto backup();

    /** Import ledger accounts from CSV (columns: code,name,type,group,openingBalance,openingSide,gstRate,gstin,hsnCode). */
    ImportResultDto importAccounts(String csv);
}
