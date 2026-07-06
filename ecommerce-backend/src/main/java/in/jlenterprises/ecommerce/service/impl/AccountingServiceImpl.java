package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.config.BillingConfig;
import in.jlenterprises.ecommerce.constant.AccountType;
import in.jlenterprises.ecommerce.constant.DrCr;
import in.jlenterprises.ecommerce.constant.PaymentMethod;
import in.jlenterprises.ecommerce.constant.PaymentStatus;
import in.jlenterprises.ecommerce.constant.VoucherType;
import in.jlenterprises.ecommerce.dto.accounting.FinancialStatementDto;
import in.jlenterprises.ecommerce.dto.accounting.JournalEntryDto;
import in.jlenterprises.ecommerce.dto.accounting.LedgerAccountDto;
import in.jlenterprises.ecommerce.dto.accounting.LedgerStatementDto;
import in.jlenterprises.ecommerce.dto.accounting.TrialBalanceDto;
import in.jlenterprises.ecommerce.entity.JournalEntry;
import in.jlenterprises.ecommerce.entity.JournalLine;
import in.jlenterprises.ecommerce.entity.LedgerAccount;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.entity.Payment;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.DuplicateResourceException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.JournalEntryRepository;
import in.jlenterprises.ecommerce.repository.JournalLineRepository;
import in.jlenterprises.ecommerce.repository.LedgerAccountRepository;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.request.accounting.JournalEntryRequest;
import in.jlenterprises.ecommerce.request.accounting.LedgerAccountRequest;
import in.jlenterprises.ecommerce.service.AccountingService;
import in.jlenterprises.ecommerce.util.GstUtil;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountingServiceImpl implements AccountingService {

    private static final Logger log = LoggerFactory.getLogger(AccountingServiceImpl.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    // Default (system) account codes used for auto-posting.
    private static final String CASH = "1000", BANK = "1010", SALES = "4000", OUTPUT_GST = "2200";

    private final LedgerAccountRepository accountRepo;
    private final JournalEntryRepository entryRepo;
    private final JournalLineRepository lineRepo;
    private final OrderRepository orderRepository;
    private final BillingConfig billingConfig;

    public AccountingServiceImpl(LedgerAccountRepository accountRepo, JournalEntryRepository entryRepo,
                                 JournalLineRepository lineRepo, OrderRepository orderRepository,
                                 BillingConfig billingConfig) {
        this.accountRepo = accountRepo;
        this.entryRepo = entryRepo;
        this.lineRepo = lineRepo;
        this.orderRepository = orderRepository;
        this.billingConfig = billingConfig;
    }

    // ── Chart of accounts ─────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<LedgerAccountDto> listAccounts(boolean includeInactive) {
        List<LedgerAccount> all = includeInactive
                ? accountRepo.findAllByOrderByCodeAsc()
                : accountRepo.findByActiveTrueOrderByCodeAsc();
        return all.stream().map(this::toAccountDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerAccountDto getAccount(UUID id) {
        return toAccountDto(account(id));
    }

    @Override
    @Transactional
    @Auditable(action = "CREATE_LEDGER", entity = "ledger_account")
    public LedgerAccountDto createAccount(LedgerAccountRequest r) {
        String code = r.code().trim();
        if (accountRepo.existsByCode(code)) throw new DuplicateResourceException("Account code already exists: " + code);
        LedgerAccount a = new LedgerAccount();
        a.setCode(code);
        apply(a, r);
        return toAccountDto(accountRepo.save(a));
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE_LEDGER", entity = "ledger_account")
    public LedgerAccountDto updateAccount(UUID id, LedgerAccountRequest r) {
        LedgerAccount a = account(id);
        if (!a.getCode().equals(r.code().trim()) && accountRepo.existsByCode(r.code().trim())) {
            throw new DuplicateResourceException("Account code already exists: " + r.code().trim());
        }
        a.setCode(r.code().trim());
        apply(a, r);
        return toAccountDto(accountRepo.save(a));
    }

    @Override
    @Transactional
    public LedgerAccountDto setBlocked(UUID id, boolean blocked) {
        LedgerAccount a = account(id);
        a.setBlocked(blocked);
        return toAccountDto(accountRepo.save(a));
    }

    @Override
    @Transactional
    public LedgerAccountDto setActive(UUID id, boolean active) {
        LedgerAccount a = account(id);
        a.setActive(active);
        return toAccountDto(accountRepo.save(a));
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_LEDGER", entity = "ledger_account")
    public void deleteAccount(UUID id) {
        LedgerAccount a = account(id);
        if (a.isSystemAccount()) throw new BusinessException("System accounts cannot be deleted — deactivate instead.");
        a.setDeleted(true);
        a.setActive(false);
        accountRepo.save(a);
    }

    // ── Journals ──────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Page<JournalEntryDto> listJournals(LocalDate from, LocalDate to, VoucherType type, Pageable pageable) {
        Specification<JournalEntry> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("entryDate"), from));
            if (to != null) ps.add(cb.lessThanOrEqualTo(root.get("entryDate"), to));
            if (type != null) ps.add(cb.equal(root.get("voucherType"), type));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return entryRepo.findAll(spec, pageable).map(this::toEntryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public JournalEntryDto getJournal(UUID id) {
        return toEntryDto(entryRepo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Journal entry", id)));
    }

    @Override
    @Transactional
    @Auditable(action = "POST_JOURNAL", entity = "journal_entry")
    public JournalEntryDto postJournal(JournalEntryRequest r) {
        if (r.lines() == null || r.lines().size() < 2) {
            throw new BusinessException("A journal entry needs at least two lines (a debit and a credit).");
        }
        BigDecimal totalDr = BigDecimal.ZERO, totalCr = BigDecimal.ZERO;
        JournalEntry entry = new JournalEntry();
        for (JournalEntryRequest.Line ln : r.lines()) {
            BigDecimal dr = nz(ln.debit()), cr = nz(ln.credit());
            if (dr.signum() < 0 || cr.signum() < 0) throw new BusinessException("Amounts cannot be negative.");
            if (dr.signum() == 0 && cr.signum() == 0) throw new BusinessException("Each line must have a debit or a credit amount.");
            if (dr.signum() > 0 && cr.signum() > 0) throw new BusinessException("A line cannot be both debit and credit.");
            LedgerAccount acc = account(ln.accountId());
            if (acc.isBlocked()) throw new BusinessException("Account is blocked for posting: " + acc.getName());
            JournalLine line = new JournalLine();
            line.setAccount(acc);
            line.setDebit(dr.setScale(2, RoundingMode.HALF_UP));
            line.setCredit(cr.setScale(2, RoundingMode.HALF_UP));
            line.setLineNarration(ln.lineNarration());
            entry.addLine(line);
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
        }
        if (totalDr.compareTo(totalCr) != 0) {
            throw new BusinessException("Entry is not balanced — debits (" + totalDr + ") must equal credits (" + totalCr + ").");
        }
        if (totalDr.signum() == 0) throw new BusinessException("Entry total cannot be zero.");

        VoucherType type = r.voucherType() == null ? VoucherType.JOURNAL : r.voucherType();
        entry.setVoucherType(type);
        entry.setEntryDate(r.entryDate());
        entry.setNarration(r.narration());
        entry.setReference(r.reference());
        entry.setVoucherNumber(nextVoucherNumber(type));
        return toEntryDto(entryRepo.save(entry));
    }

    // ── Reports ───────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public LedgerStatementDto ledgerStatement(UUID accountId, LocalDate from, LocalDate to) {
        LedgerAccount a = account(accountId);
        BigDecimal opening = openingSigned(a).add(nz(lineRepo.netMovementBefore(accountId, from)));
        BigDecimal running = opening;
        List<LedgerStatementDto.Line> lines = new ArrayList<>();
        for (JournalLine l : lineRepo.statement(accountId, from, to)) {
            running = running.add(l.getDebit()).subtract(l.getCredit());
            lines.add(new LedgerStatementDto.Line(
                    l.getJournalEntry().getEntryDate(), l.getJournalEntry().getVoucherNumber(),
                    l.getLineNarration() != null ? l.getLineNarration() : l.getJournalEntry().getNarration(),
                    l.getDebit(), l.getCredit(), running));
        }
        return new LedgerStatementDto(a.getId(), a.getCode(), a.getName(), from, to, opening, lines, running);
    }

    @Override
    @Transactional(readOnly = true)
    public TrialBalanceDto trialBalance(LocalDate asOf) {
        Map<UUID, BigDecimal[]> sums = toSumMap(lineRepo.sumByAccountAsOf(asOf));
        List<TrialBalanceDto.Row> rows = new ArrayList<>();
        BigDecimal totalDr = BigDecimal.ZERO, totalCr = BigDecimal.ZERO;
        for (LedgerAccount a : accountRepo.findAllByOrderByCodeAsc()) {
            BigDecimal net = netDebit(a, sums.get(a.getId()));   // +ve = debit balance
            if (net.signum() == 0) continue;
            BigDecimal dr = net.signum() > 0 ? net : BigDecimal.ZERO;
            BigDecimal cr = net.signum() < 0 ? net.negate() : BigDecimal.ZERO;
            rows.add(new TrialBalanceDto.Row(a.getCode(), a.getName(), a.getAccountType(), dr, cr));
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
        }
        return new TrialBalanceDto(asOf, rows, totalDr, totalCr);
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialStatementDto profitAndLoss(LocalDate from, LocalDate to) {
        Map<UUID, BigDecimal[]> sums = toSumMap(lineRepo.sumByAccountBetween(from, to));
        List<FinancialStatementDto.Line> income = new ArrayList<>(), expense = new ArrayList<>();
        BigDecimal totalIncome = BigDecimal.ZERO, totalExpense = BigDecimal.ZERO;
        for (LedgerAccount a : accountRepo.findAllByOrderByCodeAsc()) {
            BigDecimal[] s = sums.get(a.getId());
            if (s == null) continue;
            BigDecimal movementDr = s[0], movementCr = s[1];
            if (a.getAccountType() == AccountType.INCOME) {
                BigDecimal amt = movementCr.subtract(movementDr);
                if (amt.signum() != 0) { income.add(new FinancialStatementDto.Line(a.getCode(), a.getName(), amt)); totalIncome = totalIncome.add(amt); }
            } else if (a.getAccountType() == AccountType.EXPENSE) {
                BigDecimal amt = movementDr.subtract(movementCr);
                if (amt.signum() != 0) { expense.add(new FinancialStatementDto.Line(a.getCode(), a.getName(), amt)); totalExpense = totalExpense.add(amt); }
            }
        }
        List<FinancialStatementDto.Section> sections = List.of(
                new FinancialStatementDto.Section("Income", income, totalIncome),
                new FinancialStatementDto.Section("Expenses", expense, totalExpense));
        BigDecimal profit = totalIncome.subtract(totalExpense);
        return new FinancialStatementDto("Profit & Loss", from, to, sections,
                profit.signum() >= 0 ? "Net Profit" : "Net Loss", profit);
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialStatementDto balanceSheet(LocalDate asOf) {
        Map<UUID, BigDecimal[]> sums = toSumMap(lineRepo.sumByAccountAsOf(asOf));
        List<FinancialStatementDto.Line> assets = new ArrayList<>(), liabilities = new ArrayList<>(), equity = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO, totalLiab = BigDecimal.ZERO, totalEquity = BigDecimal.ZERO, retained = BigDecimal.ZERO;
        for (LedgerAccount a : accountRepo.findAllByOrderByCodeAsc()) {
            BigDecimal net = netDebit(a, sums.get(a.getId()));   // +ve = debit balance
            switch (a.getAccountType()) {
                case ASSET -> { if (net.signum() != 0) { assets.add(line(a, net)); totalAssets = totalAssets.add(net); } }
                case LIABILITY -> { BigDecimal v = net.negate(); if (v.signum() != 0) { liabilities.add(line(a, v)); totalLiab = totalLiab.add(v); } }
                case EQUITY -> { BigDecimal v = net.negate(); if (v.signum() != 0) { equity.add(line(a, v)); totalEquity = totalEquity.add(v); } }
                case INCOME, EXPENSE -> retained = retained.add(net.negate());   // credit-positive contributes to profit
            }
        }
        if (retained.signum() != 0) {
            equity.add(new FinancialStatementDto.Line("", "Current period earnings", retained));
            totalEquity = totalEquity.add(retained);
        }
        List<FinancialStatementDto.Section> sections = List.of(
                new FinancialStatementDto.Section("Assets", assets, totalAssets),
                new FinancialStatementDto.Section("Liabilities", liabilities, totalLiab),
                new FinancialStatementDto.Section("Equity", equity, totalEquity));
        BigDecimal diff = totalAssets.subtract(totalLiab.add(totalEquity));
        return new FinancialStatementDto("Balance Sheet", null, asOf, sections, "Difference", diff);
    }

    // ── Auto-post a paid sale (best-effort, isolated transaction) ──────────
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void postSaleForOrder(UUID orderId) {
        try {
            if (entryRepo.existsByReferenceIdAndVoucherType(orderId, VoucherType.SALES)) return;
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) return;
            Payment p = order.getPayment();
            if (p == null || p.getPaymentStatus() != PaymentStatus.SUCCESS) return;
            BigDecimal grand = order.getGrandTotal();
            if (grand == null || grand.signum() <= 0) return;

            LedgerAccount debitAcc = p.getMethod() == PaymentMethod.COD
                    ? accountRepo.findByCode(CASH).orElse(null) : accountRepo.findByCode(BANK).orElse(null);
            LedgerAccount salesAcc = accountRepo.findByCode(SALES).orElse(null);
            LedgerAccount gstAcc = accountRepo.findByCode(OUTPUT_GST).orElse(null);
            if (debitAcc == null || salesAcc == null || gstAcc == null) return;   // defaults not seeded

            BigDecimal gst = GstUtil.gstAmount(grand, billingConfig.gstRate());
            BigDecimal taxable = grand.subtract(gst);

            JournalEntry e = new JournalEntry();
            e.setVoucherType(VoucherType.SALES);
            e.setEntryDate(order.getPlacedAt() != null ? LocalDate.ofInstant(order.getPlacedAt(), ZONE) : LocalDate.now(ZONE));
            e.setReference(order.getOrderNumber());
            e.setReferenceId(orderId);
            e.setNarration("Sale — order " + order.getOrderNumber());
            e.setVoucherNumber(nextVoucherNumber(VoucherType.SALES));
            e.addLine(line(debitAcc, grand, BigDecimal.ZERO));
            e.addLine(line(salesAcc, BigDecimal.ZERO, taxable));
            if (gst.signum() > 0) e.addLine(line(gstAcc, BigDecimal.ZERO, gst));
            entryRepo.save(e);
        } catch (Exception ex) {
            log.warn("Auto-post of sale for order {} skipped: {}", orderId, ex.toString());
        }
    }

    // ── Post a journal from a document's postings ──────────────────────────
    @Override
    @Transactional
    public UUID postingJournal(VoucherType type, LocalDate date, String reference, UUID referenceId,
                               String narration, List<AccountingService.Posting> postings) {
        JournalEntry e = new JournalEntry();
        e.setVoucherType(type == null ? VoucherType.JOURNAL : type);
        e.setEntryDate(date);
        e.setReference(reference);
        e.setReferenceId(referenceId);
        e.setNarration(narration);
        e.setVoucherNumber(nextVoucherNumber(e.getVoucherType()));
        BigDecimal totalDr = BigDecimal.ZERO, totalCr = BigDecimal.ZERO;
        for (AccountingService.Posting p : postings) {
            BigDecimal dr = nz(p.debit()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal cr = nz(p.credit()).setScale(2, RoundingMode.HALF_UP);
            if (dr.signum() == 0 && cr.signum() == 0) continue;
            e.addLine(line(account(p.accountId()), dr, cr));
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
        }
        if (totalDr.compareTo(totalCr) != 0) {
            throw new BusinessException("Document journal is not balanced: Dr " + totalDr + " vs Cr " + totalCr);
        }
        if (totalDr.signum() == 0) throw new BusinessException("Nothing to post.");
        return entryRepo.save(e).getId();
    }

    // ── Default chart of accounts (called by the seeder on empty DB) ───────
    @Transactional
    public void ensureDefaultAccounts() {
        if (accountRepo.count() > 0) return;
        seed(CASH, "Cash-in-Hand", AccountType.ASSET, "Cash-in-Hand");
        seed(BANK, "Bank Account", AccountType.ASSET, "Bank Accounts");
        seed("1100", "Sundry Debtors", AccountType.ASSET, "Sundry Debtors");
        seed("1200", "Input GST", AccountType.ASSET, "Duties & Taxes");
        seed("2100", "Sundry Creditors", AccountType.LIABILITY, "Sundry Creditors");
        seed(OUTPUT_GST, "Output GST", AccountType.LIABILITY, "Duties & Taxes");
        seed("2300", "TDS Payable", AccountType.LIABILITY, "Duties & Taxes");
        seed("3000", "Capital Account", AccountType.EQUITY, "Capital Account");
        seed(SALES, "Sales", AccountType.INCOME, "Sales Accounts");
        seed("4100", "Shipping Income", AccountType.INCOME, "Direct Income");
        seed("5000", "Purchases", AccountType.EXPENSE, "Purchase Accounts");
        seed("5100", "Discount Allowed", AccountType.EXPENSE, "Indirect Expenses");
        seed("5900", "Round Off", AccountType.EXPENSE, "Indirect Expenses");
        log.info("Seeded default chart of accounts.");
    }

    private void seed(String code, String name, AccountType type, String group) {
        LedgerAccount a = new LedgerAccount();
        a.setCode(code); a.setName(name); a.setAccountType(type); a.setAccountGroup(group);
        a.setSystemAccount(true);
        accountRepo.save(a);
    }

    // ── helpers ────────────────────────────────────────────────────────────
    private LedgerAccount account(UUID id) {
        return accountRepo.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Ledger account", id));
    }

    private void apply(LedgerAccount a, LedgerAccountRequest r) {
        a.setName(r.name().trim());
        a.setAccountType(r.accountType());
        a.setAccountGroup(r.accountGroup());
        a.setOpeningBalance(nz(r.openingBalance()));
        a.setOpeningSide(r.openingSide() == null ? DrCr.DR : r.openingSide());
        a.setGstRate(r.gstRate());
        a.setGstin(r.gstin());
        a.setHsnCode(r.hsnCode());
        a.setCreditLimit(r.creditLimit());
        a.setCreditDays(r.creditDays());
        if (r.blocked() != null) a.setBlocked(r.blocked());
        if (r.active() != null) a.setActive(r.active());
    }

    private JournalLine line(LedgerAccount acc, BigDecimal dr, BigDecimal cr) {
        JournalLine l = new JournalLine();
        l.setAccount(acc);
        l.setDebit(dr.setScale(2, RoundingMode.HALF_UP));
        l.setCredit(cr.setScale(2, RoundingMode.HALF_UP));
        return l;
    }

    private FinancialStatementDto.Line line(LedgerAccount a, BigDecimal amount) {
        return new FinancialStatementDto.Line(a.getCode(), a.getName(), amount);
    }

    /** Signed opening balance: +ve for a debit opening, -ve for a credit opening. */
    private BigDecimal openingSigned(LedgerAccount a) {
        BigDecimal ob = nz(a.getOpeningBalance());
        return a.getOpeningSide() == DrCr.CR ? ob.negate() : ob;
    }

    /** Net debit balance (opening + movements). +ve = debit balance, -ve = credit balance. */
    private BigDecimal netDebit(LedgerAccount a, BigDecimal[] sum) {
        BigDecimal movement = sum == null ? BigDecimal.ZERO : sum[0].subtract(sum[1]);
        return openingSigned(a).add(movement);
    }

    private Map<UUID, BigDecimal[]> toSumMap(List<Object[]> rows) {
        Map<UUID, BigDecimal[]> map = new HashMap<>();
        for (Object[] r : rows) {
            map.put((UUID) r[0], new BigDecimal[]{ bd(r[1]), bd(r[2]) });
        }
        return map;
    }

    private String nextVoucherNumber(VoucherType type) {
        String prefix = switch (type) {
            case SALES -> "SV"; case PURCHASE -> "PV"; case RECEIPT -> "RV"; case PAYMENT -> "PY";
            case CONTRA -> "CV"; case CREDIT_NOTE -> "CN"; case DEBIT_NOTE -> "DN"; default -> "JV";
        };
        return prefix + "/" + (entryRepo.count() + 1);
    }

    private JournalEntryDto toEntryDto(JournalEntry e) {
        BigDecimal totalDr = BigDecimal.ZERO, totalCr = BigDecimal.ZERO;
        List<JournalEntryDto.Line> lines = new ArrayList<>();
        for (JournalLine l : e.getLines()) {
            totalDr = totalDr.add(l.getDebit());
            totalCr = totalCr.add(l.getCredit());
            LedgerAccount a = l.getAccount();
            lines.add(new JournalEntryDto.Line(l.getId(), a.getId(), a.getCode(), a.getName(),
                    l.getDebit(), l.getCredit(), l.getLineNarration()));
        }
        return new JournalEntryDto(e.getId(), e.getVoucherNumber(), e.getVoucherType(), e.getEntryDate(),
                e.getNarration(), e.getReference(), e.getReferenceId(), lines, totalDr, totalCr);
    }

    private LedgerAccountDto toAccountDto(LedgerAccount a) {
        return new LedgerAccountDto(a.getId(), a.getCode(), a.getName(), a.getAccountType(), a.getAccountGroup(),
                a.getOpeningBalance(), a.getOpeningSide(), a.getGstRate(), a.getGstin(), a.getHsnCode(),
                a.getCreditLimit(), a.getCreditDays(), a.isBlocked(), a.isActive(), a.isSystemAccount());
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static BigDecimal bd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        return new BigDecimal(o.toString());
    }
}
