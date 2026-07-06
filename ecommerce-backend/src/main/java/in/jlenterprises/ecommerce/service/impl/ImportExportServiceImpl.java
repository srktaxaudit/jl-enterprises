package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.AccountType;
import in.jlenterprises.ecommerce.constant.DocumentType;
import in.jlenterprises.ecommerce.constant.DrCr;
import in.jlenterprises.ecommerce.dto.accounting.BackupDto;
import in.jlenterprises.ecommerce.dto.accounting.DocumentSummaryDto;
import in.jlenterprises.ecommerce.dto.accounting.ImportResultDto;
import in.jlenterprises.ecommerce.dto.accounting.JournalEntryDto;
import in.jlenterprises.ecommerce.dto.accounting.LedgerAccountDto;
import in.jlenterprises.ecommerce.dto.accounting.TrialBalanceDto;
import in.jlenterprises.ecommerce.entity.LedgerAccount;
import in.jlenterprises.ecommerce.repository.LedgerAccountRepository;
import in.jlenterprises.ecommerce.service.AccountingService;
import in.jlenterprises.ecommerce.service.DocumentService;
import in.jlenterprises.ecommerce.service.ImportExportService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportExportServiceImpl implements ImportExportService {

    private static final PageRequest ALL = PageRequest.of(0, 100_000);
    private static final LocalDate EPOCH = LocalDate.of(2000, 1, 1);

    private final AccountingService accounting;
    private final DocumentService documents;
    private final LedgerAccountRepository accountRepo;

    public ImportExportServiceImpl(AccountingService accounting, DocumentService documents,
                                   LedgerAccountRepository accountRepo) {
        this.accounting = accounting;
        this.documents = documents;
        this.accountRepo = accountRepo;
    }

    // ── CSV exports ──────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public String accountsCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("Code,Name,Type,Group,Opening,Side,GST Rate,GSTIN,HSN,Credit Limit,Credit Days,Blocked,Active\n");
        for (LedgerAccountDto a : accounting.listAccounts(true)) {
            row(sb, a.code(), a.name(), a.accountType(), a.accountGroup(), num(a.openingBalance()), a.openingSide(),
                    num(a.gstRate()), a.gstin(), a.hsnCode(), num(a.creditLimit()), a.creditDays(),
                    a.blocked() ? "Yes" : "No", a.active() ? "Yes" : "No");
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String journalsCsv(LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date,Voucher,Type,Account Code,Account,Debit,Credit,Narration\n");
        for (JournalEntryDto e : accounting.listJournals(from, to, null, ALL).getContent()) {
            for (JournalEntryDto.Line l : e.lines()) {
                row(sb, e.entryDate(), e.voucherNumber(), e.voucherType(), l.accountCode(), l.accountName(),
                        num(l.debit()), num(l.credit()), e.narration());
            }
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String documentsCsv(DocumentType type, LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder();
        sb.append("Number,Date,Type,Status,Party,Taxable,GST,Grand Total\n");
        for (DocumentSummaryDto d : documents.list(type, null, from, to, null, ALL).getContent()) {
            row(sb, d.documentNumber(), d.documentDate(), d.documentType(), d.status(), d.partyName(),
                    num(d.taxableTotal()), num(d.gstTotal()), num(d.grandTotal()));
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String trialBalanceCsv(LocalDate asOf) {
        TrialBalanceDto tb = accounting.trialBalance(asOf);
        StringBuilder sb = new StringBuilder();
        sb.append("Code,Name,Type,Debit,Credit\n");
        for (TrialBalanceDto.Row r : tb.rows()) {
            row(sb, r.code(), r.name(), r.accountType(), num(r.debit()), num(r.credit()));
        }
        row(sb, "", "TOTAL", "", num(tb.totalDebit()), num(tb.totalCredit()));
        return sb.toString();
    }

    // ── Tally XML (best-effort masters + vouchers) ────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public String tallyXml(LocalDate from, LocalDate to) {
        StringBuilder sb = new StringBuilder();
        sb.append("<ENVELOPE><HEADER><TALLYREQUEST>Import Data</TALLYREQUEST></HEADER><BODY><IMPORTDATA>")
          .append("<REQUESTDESC><REPORTNAME>All Masters</REPORTNAME></REQUESTDESC><REQUESTDATA>");
        for (LedgerAccountDto a : accounting.listAccounts(true)) {
            BigDecimal ob = nz(a.openingBalance());
            String amt = (a.openingSide() == DrCr.CR ? ob : ob.negate()).toPlainString();
            sb.append("<TALLYMESSAGE><LEDGER NAME=\"").append(xml(a.name())).append("\" ACTION=\"Create\">")
              .append("<PARENT>").append(xml(tallyGroup(a.accountType()))).append("</PARENT>")
              .append("<OPENINGBALANCE>").append(amt).append("</OPENINGBALANCE>")
              .append("</LEDGER></TALLYMESSAGE>");
        }
        for (JournalEntryDto e : accounting.listJournals(from, to, null, ALL).getContent()) {
            sb.append("<TALLYMESSAGE><VOUCHER VCHTYPE=\"Journal\" ACTION=\"Create\">")
              .append("<DATE>").append(e.entryDate().toString().replace("-", "")).append("</DATE>")
              .append("<VOUCHERNUMBER>").append(xml(e.voucherNumber())).append("</VOUCHERNUMBER>")
              .append("<NARRATION>").append(xml(e.narration())).append("</NARRATION>");
            for (JournalEntryDto.Line l : e.lines()) {
                boolean debit = nz(l.debit()).signum() > 0;
                BigDecimal amt = debit ? nz(l.debit()).negate() : nz(l.credit());   // Tally: debit negative
                sb.append("<ALLLEDGERENTRIES.LIST><LEDGERNAME>").append(xml(l.accountName())).append("</LEDGERNAME>")
                  .append("<ISDEEMEDPOSITIVE>").append(debit ? "Yes" : "No").append("</ISDEEMEDPOSITIVE>")
                  .append("<AMOUNT>").append(amt.toPlainString()).append("</AMOUNT></ALLLEDGERENTRIES.LIST>");
            }
            sb.append("</VOUCHER></TALLYMESSAGE>");
        }
        sb.append("</REQUESTDATA></IMPORTDATA></BODY></ENVELOPE>");
        return sb.toString();
    }

    private String tallyGroup(AccountType t) {
        return switch (t) {
            case ASSET -> "Current Assets";
            case LIABILITY -> "Current Liabilities";
            case EQUITY -> "Capital Account";
            case INCOME -> "Sales Accounts";
            case EXPENSE -> "Indirect Expenses";
        };
    }

    // ── Backup ────────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public BackupDto backup() {
        return new BackupDto(LocalDate.now(),
                accounting.listAccounts(true),
                accounting.listJournals(EPOCH, LocalDate.now(), null, ALL).getContent(),
                documents.list(null, null, EPOCH, LocalDate.now(), null, ALL).getContent());
    }

    // ── Import: chart of accounts ─────────────────────────────────────────────
    @Override
    @Transactional
    public ImportResultDto importAccounts(String csv) {
        List<String> errors = new ArrayList<>();
        int created = 0, skipped = 0, lineNo = 0;
        if (csv == null) csv = "";
        for (String raw : csv.split("\\r?\\n")) {
            lineNo++;
            if (raw.isBlank()) continue;
            List<String> c = parseCsvLine(raw);
            if (lineNo == 1 && !c.isEmpty() && c.get(0).trim().equalsIgnoreCase("code")) continue;   // header
            String code = at(c, 0), name = at(c, 1), type = at(c, 2);
            if (code.isBlank() || name.isBlank() || type.isBlank()) {
                skipped++; errors.add("Line " + lineNo + ": code/name/type required."); continue;
            }
            // Pre-check by code so we never throw a DB unique violation (which would
            // poison the import transaction) — duplicates are simply skipped.
            if (accountRepo.existsByCode(code)) { skipped++; errors.add("Line " + lineNo + ": code '" + code + "' already exists."); continue; }
            AccountType accType;
            try { accType = AccountType.valueOf(type.trim().toUpperCase()); }
            catch (IllegalArgumentException e) { skipped++; errors.add("Line " + lineNo + ": invalid type '" + type + "'."); continue; }
            LedgerAccount a = new LedgerAccount();
            a.setCode(code);
            a.setName(name);
            a.setAccountType(accType);
            a.setAccountGroup(blankNull(at(c, 3)));
            BigDecimal ob = dec(at(c, 4));
            if (ob != null) a.setOpeningBalance(ob);
            a.setOpeningSide(side(at(c, 5)));
            a.setGstRate(dec(at(c, 6)));
            a.setGstin(blankNull(at(c, 7)));
            a.setHsnCode(blankNull(at(c, 8)));
            accountRepo.save(a);
            created++;
        }
        return new ImportResultDto(created, skipped, errors);
    }

    // ── helpers ────────────────────────────────────────────────────────────────
    private void row(StringBuilder sb, Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(esc(cells[i] == null ? "" : cells[i].toString()));
        }
        sb.append('\n');
    }

    private String esc(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private String xml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQ = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQ) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQ = false;
                } else cur.append(ch);
            } else if (ch == '"') inQ = true;
            else if (ch == ',') { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(ch);
        }
        out.add(cur.toString());
        return out;
    }

    private String at(List<String> c, int i) { return i < c.size() ? c.get(i).trim() : ""; }
    private String blankNull(String s) { return (s == null || s.isBlank()) ? null : s; }
    private BigDecimal dec(String s) { try { return (s == null || s.isBlank()) ? null : new BigDecimal(s.trim()); } catch (Exception e) { return null; } }
    private DrCr side(String s) { try { return (s == null || s.isBlank()) ? DrCr.DR : DrCr.valueOf(s.trim().toUpperCase()); } catch (Exception e) { return DrCr.DR; } }
    private static String num(BigDecimal v) { return v == null ? "0" : v.toPlainString(); }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
