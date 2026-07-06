package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.DocumentStatus;
import in.jlenterprises.ecommerce.constant.DocumentType;
import in.jlenterprises.ecommerce.constant.VoucherType;
import in.jlenterprises.ecommerce.dto.accounting.DocumentDto;
import in.jlenterprises.ecommerce.dto.accounting.DocumentSummaryDto;
import in.jlenterprises.ecommerce.entity.AccountingDocument;
import in.jlenterprises.ecommerce.entity.DocumentLine;
import in.jlenterprises.ecommerce.entity.JournalEntry;
import in.jlenterprises.ecommerce.entity.LedgerAccount;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.AccountingDocumentRepository;
import in.jlenterprises.ecommerce.repository.JournalEntryRepository;
import in.jlenterprises.ecommerce.repository.LedgerAccountRepository;
import in.jlenterprises.ecommerce.request.accounting.DocumentRequest;
import in.jlenterprises.ecommerce.service.AccountingService;
import in.jlenterprises.ecommerce.service.DocumentService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);
    // System account codes used when posting documents.
    private static final String SALES = "4000", PURCHASES = "5000", OUTPUT_GST = "2200",
            INPUT_GST = "1200", TDS = "2300";

    private final AccountingDocumentRepository docRepo;
    private final LedgerAccountRepository accountRepo;
    private final JournalEntryRepository entryRepo;
    private final AccountingService accountingService;

    public DocumentServiceImpl(AccountingDocumentRepository docRepo, LedgerAccountRepository accountRepo,
                               JournalEntryRepository entryRepo, AccountingService accountingService) {
        this.docRepo = docRepo;
        this.accountRepo = accountRepo;
        this.entryRepo = entryRepo;
        this.accountingService = accountingService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentSummaryDto> list(DocumentType type, DocumentStatus status,
                                         LocalDate from, LocalDate to, String q, Pageable pageable) {
        Specification<AccountingDocument> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (type != null) ps.add(cb.equal(root.get("documentType"), type));
            if (status != null) ps.add(cb.equal(root.get("documentStatus"), status));
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("documentDate"), from));
            if (to != null) ps.add(cb.lessThanOrEqualTo(root.get("documentDate"), to));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                ps.add(cb.or(cb.like(cb.lower(root.get("documentNumber")), like),
                        cb.like(cb.lower(root.get("partyName")), like)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return docRepo.findAll(spec, pageable).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentDto get(UUID id) {
        return toDto(doc(id));
    }

    @Override
    @Transactional
    @Auditable(action = "CREATE_DOCUMENT", entity = "accounting_document")
    public DocumentDto create(DocumentRequest r) {
        AccountingDocument d = new AccountingDocument();
        d.setDocumentType(r.documentType());
        d.setDocumentNumber(nextNumber(r.documentType()));
        apply(d, r);
        return toDto(docRepo.save(d));
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE_DOCUMENT", entity = "accounting_document")
    public DocumentDto update(UUID id, DocumentRequest r) {
        AccountingDocument d = doc(id);
        if (d.getDocumentStatus() != DocumentStatus.DRAFT) {
            throw new BusinessException("Only draft documents can be edited.");
        }
        d.getLines().clear();
        apply(d, r);
        return toDto(docRepo.save(d));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        AccountingDocument d = doc(id);
        if (d.getDocumentStatus() == DocumentStatus.POSTED) {
            throw new BusinessException("Cancel a posted document instead of deleting it.");
        }
        d.setDeleted(true);
        docRepo.save(d);
    }

    @Override
    @Transactional
    @Auditable(action = "POST_DOCUMENT", entity = "accounting_document")
    public DocumentDto post(UUID id) {
        AccountingDocument d = doc(id);
        if (d.getDocumentStatus() != DocumentStatus.DRAFT) throw new BusinessException("Only a draft can be posted.");
        if (d.getDocumentType() == DocumentType.ESTIMATE) throw new BusinessException("Convert the estimate to an invoice first.");
        if (d.getParty() == null) throw new BusinessException("Select a party ledger before posting.");

        LedgerAccount party = d.getParty();
        if (party.isBlocked()) {
            throw new BusinessException("Party ledger '" + party.getName() + "' is blocked for transactions.");
        }
        // Credit control on sales: value limit + overdue-days.
        if (d.getDocumentType() == DocumentType.SALES_INVOICE) {
            BigDecimal receivable = accountingService.netBalance(party.getId());   // +ve = amount owed to us
            BigDecimal exposure = receivable.max(BigDecimal.ZERO).add(nz(d.getGrandTotal()));
            if (party.getCreditLimit() != null && party.getCreditLimit().signum() > 0
                    && exposure.compareTo(party.getCreditLimit()) > 0) {
                throw new BusinessException("Credit limit exceeded for " + party.getName()
                        + ": limit ₹" + party.getCreditLimit() + ", outstanding would become ₹" + exposure
                        + ". Collect dues or raise the limit.");
            }
            if (party.getCreditDays() != null && party.getCreditDays() > 0 && receivable.signum() > 0) {
                LocalDate oldest = docRepo.oldestSalesInvoiceDate(party.getId());
                if (oldest != null && java.time.temporal.ChronoUnit.DAYS.between(oldest, d.getDocumentDate()) > party.getCreditDays()) {
                    throw new BusinessException(party.getName() + " has invoices overdue beyond "
                            + party.getCreditDays() + " days. Collect the outstanding before billing again.");
                }
            }
        }

        UUID journalId = accountingService.postingJournal(voucherType(d.getDocumentType()), d.getDocumentDate(),
                d.getDocumentNumber(), d.getId(), narration(d), buildPostings(d));
        d.setJournalEntryId(journalId);
        d.setDocumentStatus(DocumentStatus.POSTED);
        return toDto(docRepo.save(d));
    }

    @Override
    @Transactional
    @Auditable(action = "CANCEL_DOCUMENT", entity = "accounting_document")
    public DocumentDto cancel(UUID id) {
        AccountingDocument d = doc(id);
        if (d.getDocumentStatus() == DocumentStatus.CANCELLED) return toDto(d);
        if (d.getJournalEntryId() != null) {
            // Remove its journal from the ledgers (soft-delete keeps an audit trail).
            entryRepo.findById(d.getJournalEntryId()).ifPresent(e -> { e.setDeleted(true); entryRepo.save(e); });
            d.setJournalEntryId(null);
        }
        d.setDocumentStatus(DocumentStatus.CANCELLED);
        return toDto(docRepo.save(d));
    }

    @Override
    @Transactional
    public DocumentDto convertEstimate(UUID id) {
        AccountingDocument est = doc(id);
        if (est.getDocumentType() != DocumentType.ESTIMATE) throw new BusinessException("Only an estimate can be converted.");
        if (est.getDocumentStatus() == DocumentStatus.CONVERTED) throw new BusinessException("This estimate is already converted.");

        AccountingDocument inv = new AccountingDocument();
        inv.setDocumentType(DocumentType.SALES_INVOICE);
        inv.setDocumentNumber(nextNumber(DocumentType.SALES_INVOICE));
        inv.setDocumentDate(LocalDate.now());
        inv.setParty(est.getParty());
        inv.setPartyName(est.getPartyName());
        inv.setPartyGstin(est.getPartyGstin());
        inv.setBillingAddress(est.getBillingAddress());
        inv.setShippingAddress(est.getShippingAddress());
        inv.setPlaceOfSupply(est.getPlaceOfSupply());
        inv.setInterState(est.isInterState());
        inv.setNarration(est.getNarration());
        inv.setReference(est.getDocumentNumber());
        inv.setReferenceDocumentId(est.getId());
        for (DocumentLine el : est.getLines()) {
            DocumentLine nl = new DocumentLine();
            nl.setDescription(el.getDescription());
            nl.setHsnCode(el.getHsnCode());
            nl.setProductId(el.getProductId());
            nl.setQuantity(el.getQuantity());
            nl.setRate(el.getRate());
            nl.setDiscountPercent(el.getDiscountPercent());
            nl.setGstRate(el.getGstRate());
            inv.addLine(nl);
        }
        recompute(inv);
        est.setDocumentStatus(DocumentStatus.CONVERTED);
        docRepo.save(est);
        return toDto(docRepo.save(inv));
    }

    // ── posting ────────────────────────────────────────────────────────────
    private List<AccountingService.Posting> buildPostings(AccountingDocument d) {
        LedgerAccount party = d.getParty();
        BigDecimal grand = nz(d.getGrandTotal()), taxable = nz(d.getTaxableTotal()),
                gst = nz(d.getGstTotal()), tds = nz(d.getTdsAmount());
        List<AccountingService.Posting> ps = new ArrayList<>();
        switch (d.getDocumentType()) {
            case SALES_INVOICE -> {
                ps.add(dr(party.getId(), grand));
                ps.add(cr(acc(SALES), taxable));
                if (gst.signum() > 0) ps.add(cr(acc(OUTPUT_GST), gst));
            }
            case PURCHASE_BILL -> {
                ps.add(dr(acc(PURCHASES), taxable));
                if (gst.signum() > 0) ps.add(dr(acc(INPUT_GST), gst));
                ps.add(cr(party.getId(), grand.subtract(tds)));
                if (tds.signum() > 0) ps.add(cr(acc(TDS), tds));
            }
            case CREDIT_NOTE -> {   // sales return
                ps.add(cr(party.getId(), grand));
                ps.add(dr(acc(SALES), taxable));
                if (gst.signum() > 0) ps.add(dr(acc(OUTPUT_GST), gst));
            }
            case DEBIT_NOTE -> {    // purchase return
                ps.add(dr(party.getId(), grand));
                ps.add(cr(acc(PURCHASES), taxable));
                if (gst.signum() > 0) ps.add(cr(acc(INPUT_GST), gst));
            }
            default -> throw new BusinessException("This document type cannot be posted.");
        }
        // Auto-balance any rounding difference to the Round Off account.
        BigDecimal dr = BigDecimal.ZERO, cr = BigDecimal.ZERO;
        for (AccountingService.Posting p : ps) { dr = dr.add(nz(p.debit())); cr = cr.add(nz(p.credit())); }
        BigDecimal diff = dr.subtract(cr);
        if (diff.signum() != 0) {
            UUID roundOff = acc("5900");
            ps.add(diff.signum() > 0 ? cr(roundOff, diff) : dr(roundOff, diff.negate()));
        }
        return ps;
    }

    private VoucherType voucherType(DocumentType t) {
        return switch (t) {
            case SALES_INVOICE -> VoucherType.SALES;
            case PURCHASE_BILL -> VoucherType.PURCHASE;
            case CREDIT_NOTE -> VoucherType.CREDIT_NOTE;
            case DEBIT_NOTE -> VoucherType.DEBIT_NOTE;
            default -> VoucherType.JOURNAL;
        };
    }

    // ── mapping / totals ─────────────────────────────────────────────────────
    private void apply(AccountingDocument d, DocumentRequest r) {
        d.setDocumentDate(r.documentDate());
        if (r.partyAccountId() != null) {
            d.setParty(accountRepo.findById(r.partyAccountId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Party account", r.partyAccountId())));
            if (d.getPartyName() == null || d.getPartyName().isBlank()) d.setPartyName(d.getParty().getName());
        }
        if (r.partyName() != null && !r.partyName().isBlank()) d.setPartyName(r.partyName());
        d.setPartyGstin(r.partyGstin());
        d.setBillingAddress(r.billingAddress());
        d.setShippingAddress(r.shippingAddress());
        d.setPlaceOfSupply(r.placeOfSupply());
        d.setInterState(Boolean.TRUE.equals(r.interState()));
        d.setNarration(r.narration());
        d.setReference(r.reference());
        d.setReferenceDocumentId(r.referenceDocumentId());
        d.setTdsSection(r.tdsSection());
        d.setTdsRate(r.tdsRate());
        for (DocumentRequest.Line lr : r.lines()) {
            DocumentLine l = new DocumentLine();
            l.setDescription(lr.description() == null ? "" : lr.description());
            l.setHsnCode(lr.hsnCode());
            l.setProductId(lr.productId());
            l.setQuantity(lr.quantity() == null ? BigDecimal.ONE : lr.quantity());
            l.setRate(nz(lr.rate()));
            l.setDiscountPercent(nz(lr.discountPercent()));
            l.setGstRate(nz(lr.gstRate()));
            d.addLine(l);
        }
        recompute(d);
    }

    /** Compute per-line tax (multi-rate, CGST/SGST or IGST) and document totals. */
    private void recompute(AccountingDocument d) {
        BigDecimal sub = BigDecimal.ZERO, disc = BigDecimal.ZERO, taxable = BigDecimal.ZERO,
                cgst = BigDecimal.ZERO, sgst = BigDecimal.ZERO, igst = BigDecimal.ZERO;
        boolean inter = d.isInterState();
        for (DocumentLine l : d.getLines()) {
            BigDecimal base = nz(l.getQuantity()).multiply(nz(l.getRate()));
            BigDecimal d1 = base.multiply(nz(l.getDiscountPercent())).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal tv = base.subtract(d1).setScale(2, RoundingMode.HALF_UP);
            BigDecimal g = tv.multiply(nz(l.getGstRate())).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal lc = BigDecimal.ZERO, ls = BigDecimal.ZERO, li = BigDecimal.ZERO;
            if (inter) { li = g; } else { lc = g.divide(TWO, 2, RoundingMode.HALF_UP); ls = g.subtract(lc); }
            l.setTaxableValue(tv); l.setCgst(lc); l.setSgst(ls); l.setIgst(li);
            l.setLineTotal(tv.add(g));
            sub = sub.add(base); disc = disc.add(d1); taxable = taxable.add(tv);
            cgst = cgst.add(lc); sgst = sgst.add(ls); igst = igst.add(li);
        }
        BigDecimal gst = cgst.add(sgst).add(igst);
        BigDecimal rawGrand = taxable.add(gst);
        BigDecimal grand = rawGrand.setScale(0, RoundingMode.HALF_UP);
        BigDecimal tds = (d.getTdsRate() != null && d.getTdsRate().signum() > 0)
                ? taxable.multiply(d.getTdsRate()).divide(HUNDRED, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        d.setSubTotal(sub.setScale(2, RoundingMode.HALF_UP));
        d.setDiscountTotal(disc); d.setTaxableTotal(taxable);
        d.setCgstTotal(cgst); d.setSgstTotal(sgst); d.setIgstTotal(igst); d.setGstTotal(gst);
        d.setRoundOff(grand.subtract(rawGrand).setScale(2, RoundingMode.HALF_UP));
        d.setGrandTotal(grand.setScale(2, RoundingMode.HALF_UP));
        d.setTdsAmount(tds);
    }

    private String nextNumber(DocumentType t) {
        String prefix = switch (t) {
            case SALES_INVOICE -> "INV"; case PURCHASE_BILL -> "PUR"; case ESTIMATE -> "EST";
            case CREDIT_NOTE -> "CN"; case DEBIT_NOTE -> "DN";
        };
        return prefix + "/" + (docRepo.countByDocumentType(t) + 1);
    }

    private String narration(AccountingDocument d) {
        return d.getDocumentType() + " " + d.getDocumentNumber()
                + (d.getPartyName() != null ? " — " + d.getPartyName() : "");
    }

    private AccountingDocument doc(UUID id) {
        return docRepo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Document", id));
    }

    private UUID acc(String code) {
        return accountRepo.findByCode(code)
                .orElseThrow(() -> new BusinessException("Required ledger account missing (code " + code
                        + "). Ensure the default chart of accounts is seeded."))
                .getId();
    }

    private AccountingService.Posting dr(UUID accountId, BigDecimal amount) {
        return new AccountingService.Posting(accountId, amount, BigDecimal.ZERO);
    }

    private AccountingService.Posting cr(UUID accountId, BigDecimal amount) {
        return new AccountingService.Posting(accountId, BigDecimal.ZERO, amount);
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private DocumentSummaryDto toSummary(AccountingDocument d) {
        return new DocumentSummaryDto(d.getId(), d.getDocumentType(), d.getDocumentNumber(), d.getDocumentDate(),
                d.getDocumentStatus(), d.getPartyName(), d.getTaxableTotal(), d.getGstTotal(), d.getGrandTotal());
    }

    private DocumentDto toDto(AccountingDocument d) {
        List<DocumentDto.Line> lines = new ArrayList<>();
        for (DocumentLine l : d.getLines()) {
            lines.add(new DocumentDto.Line(l.getId(), l.getDescription(), l.getHsnCode(), l.getProductId(),
                    l.getQuantity(), l.getRate(), l.getDiscountPercent(), l.getGstRate(),
                    l.getTaxableValue(), l.getCgst(), l.getSgst(), l.getIgst(), l.getLineTotal()));
        }
        return new DocumentDto(d.getId(), d.getDocumentType(), d.getDocumentNumber(), d.getDocumentDate(),
                d.getDocumentStatus(), d.getParty() == null ? null : d.getParty().getId(), d.getPartyName(),
                d.getPartyGstin(), d.getBillingAddress(), d.getShippingAddress(), d.getPlaceOfSupply(),
                d.isInterState(), d.getNarration(), d.getReference(), d.getReferenceDocumentId(),
                d.getTdsSection(), d.getTdsRate(), d.getTdsAmount(),
                d.getSubTotal(), d.getDiscountTotal(), d.getTaxableTotal(), d.getCgstTotal(), d.getSgstTotal(),
                d.getIgstTotal(), d.getGstTotal(), d.getRoundOff(), d.getGrandTotal(), d.getJournalEntryId(), lines);
    }
}
