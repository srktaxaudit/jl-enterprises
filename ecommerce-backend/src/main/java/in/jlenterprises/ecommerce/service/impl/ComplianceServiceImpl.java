package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.config.BillingConfig;
import in.jlenterprises.ecommerce.constant.DocumentType;
import in.jlenterprises.ecommerce.constant.DrCr;
import in.jlenterprises.ecommerce.dto.accounting.AgingReportDto;
import in.jlenterprises.ecommerce.dto.accounting.CashFlowDto;
import in.jlenterprises.ecommerce.dto.accounting.EwayBillDto;
import in.jlenterprises.ecommerce.dto.accounting.GstReturnDto;
import in.jlenterprises.ecommerce.dto.accounting.Gstr3bDto;
import in.jlenterprises.ecommerce.entity.AccountingDocument;
import in.jlenterprises.ecommerce.entity.DocumentLine;
import in.jlenterprises.ecommerce.entity.LedgerAccount;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.AccountingDocumentRepository;
import in.jlenterprises.ecommerce.repository.JournalLineRepository;
import in.jlenterprises.ecommerce.repository.LedgerAccountRepository;
import in.jlenterprises.ecommerce.service.ComplianceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ComplianceServiceImpl implements ComplianceService {

    private static final BigDecimal EWAY_THRESHOLD = new BigDecimal("50000");
    private static final String CASH = "1000", BANK = "1010";

    private final AccountingDocumentRepository docRepo;
    private final JournalLineRepository lineRepo;
    private final LedgerAccountRepository accountRepo;
    private final BillingConfig billingConfig;

    public ComplianceServiceImpl(AccountingDocumentRepository docRepo, JournalLineRepository lineRepo,
                                 LedgerAccountRepository accountRepo, BillingConfig billingConfig) {
        this.docRepo = docRepo;
        this.lineRepo = lineRepo;
        this.accountRepo = accountRepo;
        this.billingConfig = billingConfig;
    }

    // ── GSTR-1 (outward) ────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public GstReturnDto gstr1(LocalDate from, LocalDate to) {
        List<AccountingDocument> docs = docRepo.findPostedWithLines(
                List.of(DocumentType.SALES_INVOICE, DocumentType.CREDIT_NOTE), from, to);
        return buildReturn("GSTR-1", from, to, docs, DocumentType.SALES_INVOICE, DocumentType.CREDIT_NOTE,
                "B2B (registered)", "B2C (unregistered)", "Credit Notes");
    }

    // ── GSTR-2 (inward) ─────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public GstReturnDto gstr2(LocalDate from, LocalDate to) {
        List<AccountingDocument> docs = docRepo.findPostedWithLines(
                List.of(DocumentType.PURCHASE_BILL, DocumentType.DEBIT_NOTE), from, to);
        return buildReturn("GSTR-2", from, to, docs, DocumentType.PURCHASE_BILL, DocumentType.DEBIT_NOTE,
                "Purchases (registered)", "Purchases (unregistered)", "Debit Notes");
    }

    private GstReturnDto buildReturn(String type, LocalDate from, LocalDate to, List<AccountingDocument> docs,
                                     DocumentType mainType, DocumentType noteType,
                                     String regTitle, String unregTitle, String noteTitle) {
        List<GstReturnDto.Row> reg = new ArrayList<>(), unreg = new ArrayList<>(), notes = new ArrayList<>();
        Map<String, BigDecimal[]> hsn = new LinkedHashMap<>();
        for (AccountingDocument d : docs) {
            GstReturnDto.Row row = new GstReturnDto.Row(d.getDocumentNumber(), d.getDocumentDate(),
                    d.getPartyName(), d.getPartyGstin(),
                    nz(d.getTaxableTotal()), nz(d.getCgstTotal()), nz(d.getSgstTotal()), nz(d.getIgstTotal()), nz(d.getGrandTotal()));
            boolean isNote = d.getDocumentType() == noteType;
            if (isNote) notes.add(row);
            else if (d.getPartyGstin() != null && !d.getPartyGstin().isBlank()) reg.add(row);
            else unreg.add(row);
            int sign = isNote ? -1 : 1;
            for (DocumentLine l : d.getLines()) {
                String key = (l.getHsnCode() == null ? "—" : l.getHsnCode()) + "|" + nz(l.getGstRate()).toPlainString();
                BigDecimal[] a = hsn.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
                a[0] = a[0].add(nz(l.getTaxableValue()).multiply(BigDecimal.valueOf(sign)));
                a[1] = a[1].add(nz(l.getCgst()).multiply(BigDecimal.valueOf(sign)));
                a[2] = a[2].add(nz(l.getSgst()).multiply(BigDecimal.valueOf(sign)));
                a[3] = a[3].add(nz(l.getIgst()).multiply(BigDecimal.valueOf(sign)));
            }
        }
        List<GstReturnDto.Section> sections = List.of(
                section(regTitle, reg), section(unregTitle, unreg), section(noteTitle, notes));
        List<GstReturnDto.HsnRow> hsnRows = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : hsn.entrySet()) {
            String[] parts = e.getKey().split("\\|");
            BigDecimal[] a = e.getValue();
            hsnRows.add(new GstReturnDto.HsnRow(parts[0], new BigDecimal(parts[1]),
                    a[0], a[1], a[2], a[3], a[0].add(a[1]).add(a[2]).add(a[3])));
        }
        // Net totals = main documents minus notes.
        BigDecimal[] net = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        for (AccountingDocument d : docs) {
            int sign = d.getDocumentType() == noteType ? -1 : 1;
            net[0] = net[0].add(nz(d.getTaxableTotal()).multiply(BigDecimal.valueOf(sign)));
            net[1] = net[1].add(nz(d.getCgstTotal()).multiply(BigDecimal.valueOf(sign)));
            net[2] = net[2].add(nz(d.getSgstTotal()).multiply(BigDecimal.valueOf(sign)));
            net[3] = net[3].add(nz(d.getIgstTotal()).multiply(BigDecimal.valueOf(sign)));
        }
        return new GstReturnDto(type, from, to, net[0], net[1], net[2], net[3],
                net[1].add(net[2]).add(net[3]), sections, hsnRows);
    }

    private GstReturnDto.Section section(String title, List<GstReturnDto.Row> rows) {
        BigDecimal t = BigDecimal.ZERO, c = BigDecimal.ZERO, s = BigDecimal.ZERO, i = BigDecimal.ZERO;
        for (GstReturnDto.Row r : rows) { t = t.add(r.taxable()); c = c.add(r.cgst()); s = s.add(r.sgst()); i = i.add(r.igst()); }
        return new GstReturnDto.Section(title, rows, t, c, s, i);
    }

    // ── GSTR-3B ─────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Gstr3bDto gstr3b(LocalDate from, LocalDate to) {
        BigDecimal[] out = net(DocumentType.SALES_INVOICE, DocumentType.CREDIT_NOTE, from, to);
        BigDecimal[] in = net(DocumentType.PURCHASE_BILL, DocumentType.DEBIT_NOTE, from, to);
        BigDecimal netC = out[1].subtract(in[1]), netS = out[2].subtract(in[2]), netI = out[3].subtract(in[3]);
        return new Gstr3bDto(from, to, out[0], out[1], out[2], out[3], in[0], in[1], in[2], in[3],
                netC, netS, netI, netC.add(netS).add(netI));
    }

    private BigDecimal[] net(DocumentType mainType, DocumentType noteType, LocalDate from, LocalDate to) {
        BigDecimal[] a = new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        for (AccountingDocument d : docRepo.findPostedWithLines(List.of(mainType, noteType), from, to)) {
            int sign = d.getDocumentType() == noteType ? -1 : 1;
            a[0] = a[0].add(nz(d.getTaxableTotal()).multiply(BigDecimal.valueOf(sign)));
            a[1] = a[1].add(nz(d.getCgstTotal()).multiply(BigDecimal.valueOf(sign)));
            a[2] = a[2].add(nz(d.getSgstTotal()).multiply(BigDecimal.valueOf(sign)));
            a[3] = a[3].add(nz(d.getIgstTotal()).multiply(BigDecimal.valueOf(sign)));
        }
        return a;
    }

    // ── Cash flow (cash & bank movements) ────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public CashFlowDto cashFlow(LocalDate from, LocalDate to) {
        List<CashFlowDto.Row> inflows = new ArrayList<>(), outflows = new ArrayList<>();
        BigDecimal opening = BigDecimal.ZERO, totalIn = BigDecimal.ZERO, totalOut = BigDecimal.ZERO;
        for (String code : new String[]{CASH, BANK}) {
            LedgerAccount acc = accountRepo.findByCode(code).orElse(null);
            if (acc == null) continue;
            BigDecimal os = acc.getOpeningSide() == DrCr.CR ? nz(acc.getOpeningBalance()).negate() : nz(acc.getOpeningBalance());
            opening = opening.add(os).add(nz(lineRepo.netMovementBefore(acc.getId(), from)));
            for (var l : lineRepo.statement(acc.getId(), from, to)) {
                String particulars = l.getJournalEntry().getNarration() != null
                        ? l.getJournalEntry().getNarration() : l.getJournalEntry().getVoucherNumber();
                if (l.getDebit().signum() > 0) {
                    inflows.add(new CashFlowDto.Row(l.getJournalEntry().getEntryDate(),
                            l.getJournalEntry().getVoucherNumber(), particulars, l.getDebit()));
                    totalIn = totalIn.add(l.getDebit());
                } else if (l.getCredit().signum() > 0) {
                    outflows.add(new CashFlowDto.Row(l.getJournalEntry().getEntryDate(),
                            l.getJournalEntry().getVoucherNumber(), particulars, l.getCredit()));
                    totalOut = totalOut.add(l.getCredit());
                }
            }
        }
        return new CashFlowDto(from, to, opening, inflows, outflows, totalIn, totalOut,
                opening.add(totalIn).subtract(totalOut));
    }

    // ── Aging (outstanding by days) ──────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public AgingReportDto aging(String kind, LocalDate asOf) {
        boolean receivable = !"PAYABLE".equalsIgnoreCase(kind);
        DocumentType mainType = receivable ? DocumentType.SALES_INVOICE : DocumentType.PURCHASE_BILL;
        DocumentType noteType = receivable ? DocumentType.CREDIT_NOTE : DocumentType.DEBIT_NOTE;
        List<AccountingDocument> docs = docRepo.findPostedByTypesAsOf(List.of(mainType, noteType), asOf);

        Map<String, BigDecimal[]> buckets = new LinkedHashMap<>();   // [0-30,31-60,61-90,90+, oldestDays]
        for (AccountingDocument d : docs) {
            String name = d.getPartyName() != null ? d.getPartyName()
                    : (d.getParty() != null ? d.getParty().getName() : "—");
            int sign = d.getDocumentType() == noteType ? -1 : 1;
            BigDecimal amt = nz(d.getGrandTotal()).multiply(BigDecimal.valueOf(sign));
            long age = ChronoUnit.DAYS.between(d.getDocumentDate(), asOf);
            int b = age <= 30 ? 0 : age <= 60 ? 1 : age <= 90 ? 2 : 3;
            BigDecimal[] arr = buckets.computeIfAbsent(name, k ->
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            arr[b] = arr[b].add(amt);
            if (sign > 0 && age > arr[4].longValue()) arr[4] = BigDecimal.valueOf(age);
        }

        // Carried-forward party opening balances (e.g. migrated from Vyapar) have no
        // invoice date, so they don't appear as documents above. Add them here as
        // brought-forward dues in the oldest (90+) bucket so this report reflects the
        // real outstanding. Uses the Sundry Debtors/Creditors group + opening side.
        String group = receivable ? "Sundry Debtors" : "Sundry Creditors";
        DrCr side = receivable ? DrCr.DR : DrCr.CR;
        for (LedgerAccount a : accountRepo.findAll()) {
            BigDecimal ob = nz(a.getOpeningBalance());
            if (ob.signum() <= 0 || a.getOpeningSide() != side) continue;
            if (!group.equalsIgnoreCase(a.getAccountGroup())) continue;
            BigDecimal[] arr = buckets.computeIfAbsent(a.getName(), k ->
                    new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            arr[3] = arr[3].add(ob);                       // 90+ (opening / brought forward)
            arr[4] = arr[4].max(BigDecimal.valueOf(90));
        }

        List<AgingReportDto.PartyRow> rows = new ArrayList<>();
        BigDecimal c0 = BigDecimal.ZERO, c1 = BigDecimal.ZERO, c2 = BigDecimal.ZERO, c3 = BigDecimal.ZERO, tot = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal[]> e : buckets.entrySet()) {
            BigDecimal[] a = e.getValue();
            BigDecimal total = a[0].add(a[1]).add(a[2]).add(a[3]);
            if (total.abs().compareTo(new BigDecimal("0.01")) < 0) continue;
            rows.add(new AgingReportDto.PartyRow(e.getKey(), a[0], a[1], a[2], a[3], total, a[4].intValue()));
            c0 = c0.add(a[0]); c1 = c1.add(a[1]); c2 = c2.add(a[2]); c3 = c3.add(a[3]); tot = tot.add(total);
        }
        rows.sort((x, y) -> y.total().compareTo(x.total()));
        return new AgingReportDto(receivable ? "RECEIVABLE" : "PAYABLE", asOf, rows, c0, c1, c2, c3, tot);
    }

    // ── E-way bill data ───────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public EwayBillDto ewayBill(UUID documentId) {
        AccountingDocument d = docRepo.findById(documentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Document", documentId));
        boolean outward = d.getDocumentType().isSalesSide();
        String sellerGstin = billingConfig.sellerGstin(), sellerName = billingConfig.sellerName(), sellerAddr = billingConfig.sellerAddress();
        String fromGstin = outward ? sellerGstin : d.getPartyGstin();
        String fromName = outward ? sellerName : d.getPartyName();
        String fromAddr = outward ? sellerAddr : d.getBillingAddress();
        String toGstin = outward ? d.getPartyGstin() : sellerGstin;
        String toName = outward ? d.getPartyName() : sellerName;
        String toAddr = outward ? (d.getShippingAddress() != null ? d.getShippingAddress() : d.getBillingAddress()) : sellerAddr;
        List<EwayBillDto.Item> items = new ArrayList<>();
        for (DocumentLine l : d.getLines()) {
            items.add(new EwayBillDto.Item(l.getDescription(), l.getHsnCode(), l.getQuantity(), l.getTaxableValue(), l.getGstRate()));
        }
        boolean eligible = nz(d.getGrandTotal()).compareTo(EWAY_THRESHOLD) >= 0;
        String note = eligible
                ? "Invoice value ≥ ₹50,000 — an e-way bill is generally required. Use this data on the NIC e-way bill portal."
                : "Invoice value below ₹50,000 — an e-way bill is generally not mandatory.";
        return new EwayBillDto(d.getDocumentNumber(), d.getDocumentDate(), outward ? "OUTWARD" : "INWARD",
                fromGstin, fromName, fromAddr, toGstin, toName, toAddr,
                nz(d.getTaxableTotal()), nz(d.getCgstTotal()), nz(d.getSgstTotal()), nz(d.getIgstTotal()),
                nz(d.getGrandTotal()), eligible, note, items);
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
