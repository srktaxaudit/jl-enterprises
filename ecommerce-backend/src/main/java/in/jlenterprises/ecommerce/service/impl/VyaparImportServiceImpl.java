package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.AccountType;
import in.jlenterprises.ecommerce.constant.AddressType;
import in.jlenterprises.ecommerce.constant.AuthProvider;
import in.jlenterprises.ecommerce.constant.DrCr;
import in.jlenterprises.ecommerce.constant.RoleName;
import in.jlenterprises.ecommerce.dto.migration.MigrationResult;
import in.jlenterprises.ecommerce.entity.Address;
import in.jlenterprises.ecommerce.entity.Category;
import in.jlenterprises.ecommerce.entity.Inventory;
import in.jlenterprises.ecommerce.entity.LedgerAccount;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.entity.Role;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.repository.AddressRepository;
import in.jlenterprises.ecommerce.repository.CategoryRepository;
import in.jlenterprises.ecommerce.repository.InventoryRepository;
import in.jlenterprises.ecommerce.repository.LedgerAccountRepository;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import in.jlenterprises.ecommerce.repository.RoleRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.request.migration.VyaparPackage;
import in.jlenterprises.ecommerce.service.VyaparImportService;
import in.jlenterprises.ecommerce.util.SlugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class VyaparImportServiceImpl implements VyaparImportService {

    private static final Logger log = LoggerFactory.getLogger(VyaparImportServiceImpl.class);

    // Seeded chart-of-accounts codes we set opening balances on (see AccountingServiceImpl).
    private static final String CASH = "1000", BANK = "1010", CAPITAL = "3000", STOCK = "1300";
    private static final String VYP_PRODUCT_PREFIX = "VYP-";
    private static final String VYP_PARTY_PREFIX = "VYP-P-";
    private static final int MAX_WARNINGS = 40;

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;
    private final LedgerAccountRepository accountRepo;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressRepository addressRepository;

    public VyaparImportServiceImpl(ProductRepository productRepository, InventoryRepository inventoryRepository,
                                   CategoryRepository categoryRepository, LedgerAccountRepository accountRepo,
                                   UserRepository userRepository, RoleRepository roleRepository,
                                   AddressRepository addressRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepo = accountRepo;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.addressRepository = addressRepository;
    }

    @Override
    @Transactional
    public MigrationResult run(VyaparPackage pkg, boolean dryRun) {
        List<String> warnings = new ArrayList<>();

        // ── 1. Catalogue + stock ──────────────────────────────────────────
        int created = 0, updated = 0, skipped = 0;
        Map<String, Category> catCache = new HashMap<>();
        for (VyaparPackage.Product vp : nz(pkg.products())) {
            String sku = trunc(vp.sku(), 80);
            BigDecimal price = vp.price();
            if (sku == null || sku.isBlank() || price == null || price.signum() <= 0) {
                skipped++;
                addWarning(warnings, "Skipped product (no SKU/price): " + (vp.name() == null ? sku : vp.name()));
                continue;
            }
            Optional<Product> existing = productRepository.findBySku(sku);
            if (existing.isPresent()) {
                updated++;
                if (!dryRun) applyProduct(existing.get(), vp, catCache, dryRun);
            } else {
                created++;
                if (!dryRun) {
                    Product p = new Product();
                    p.setSku(sku);
                    p.setSlug(uniqueSlug(vp.name(), sku));
                    applyProduct(p, vp, catCache, dryRun);
                }
            }
        }

        // ── 2. Party ledgers (opening balances by sign) ───────────────────
        int ledgers = 0;
        BigDecimal receivables = BigDecimal.ZERO, payables = BigDecimal.ZERO;
        for (VyaparPackage.Party party : nz(pkg.parties())) {
            BigDecimal ob = nz(party.openingBalance());
            if (ob.signum() <= 0) continue;                 // settled party → no ledger needed
            boolean dr = !"CR".equalsIgnoreCase(party.openingSide());
            ledgers++;
            if (dr) receivables = receivables.add(ob); else payables = payables.add(ob);
            if (!dryRun) upsertLedger(party, ob, dr);
        }

        // ── 3. Customer contacts (CRM / marketing) ────────────────────────
        int contactsCreated = 0, contactsSkipped = 0;
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER).orElse(null);
        Set<String> batchPhones = new HashSet<>();
        for (VyaparPackage.Party party : nz(pkg.parties())) {
            if (!"CUSTOMER".equalsIgnoreCase(party.type())) continue;   // suppliers = ledger only
            String phone = digits(party.phone());
            if (phone == null || phone.length() < 10) continue;         // only reachable contacts
            String last10 = last10(phone);
            String tag = party.code();
            if (!batchPhones.add(last10)) { contactsSkipped++; continue; }   // dup within this file
            List<User> byPhone = userRepository.findByPhoneLast10(last10);
            User mine = byPhone.stream().filter(u -> tag.equals(u.getProviderId())).findFirst().orElse(null);
            boolean foreign = byPhone.stream().anyMatch(u -> !tag.equals(u.getProviderId()));
            if (mine == null && foreign) { contactsSkipped++; continue; }     // don't duplicate a storefront customer
            contactsCreated++;
            if (!dryRun) upsertContact(party, mine, phone, tag, customerRole);
        }

        // ── 4. Opening trial balance ──────────────────────────────────────
        BigDecimal cash = nz(pkg.opening() == null ? null : pkg.opening().cash());
        BigDecimal bank = nz(pkg.opening() == null ? null : pkg.opening().bank());
        BigDecimal stock = nz(pkg.opening() == null ? null : pkg.opening().stockValue());

        BigDecimal totalDr = receivables.add(stock).add(cash).add(bank);
        BigDecimal capital = totalDr.subtract(payables);          // balancing figure (owner's net worth)
        boolean capitalCredit = capital.signum() >= 0;
        BigDecimal totalCr = payables.add(capital.max(BigDecimal.ZERO));
        if (!capitalCredit) {                                      // defensive: net liabilities
            totalDr = totalDr.add(capital.abs());
            totalCr = payables;
            addWarning(warnings, "Capital balancing figure is negative (net liabilities) — please review before committing.");
        }
        boolean trialBalanceOk = totalDr.compareTo(totalCr) == 0;

        if (!dryRun) {
            setOpening(CASH, cash, DrCr.DR);
            setOpening(BANK, bank, DrCr.DR);
            ensureOpeningStock(stock);
            setOpening(CAPITAL, capital.abs(), capitalCredit ? DrCr.CR : DrCr.DR);
            log.info("Vyapar import committed: {} products (+{} updated), {} ledgers, {} contacts.",
                    created, updated, ledgers, contactsCreated);
        }

        return new MigrationResult(dryRun, created, updated, skipped, ledgers,
                receivables, payables, contactsCreated, contactsSkipped,
                cash, bank, stock, capital, totalDr, totalCr, trialBalanceOk, warnings);
    }

    @Override
    @Transactional
    public MigrationResult rollback() {
        List<String> warnings = new ArrayList<>();

        List<Product> products = productRepository.findBySkuStartingWith(VYP_PRODUCT_PREFIX);
        for (Product p : products) {
            inventoryRepository.findByProductId(p.getId()).ifPresent(inv -> { inv.setDeleted(true); inventoryRepository.save(inv); });
            p.setDeleted(true);
            productRepository.save(p);
        }
        List<LedgerAccount> ledgers = accountRepo.findByCodeStartingWith(VYP_PARTY_PREFIX);
        for (LedgerAccount a : ledgers) { a.setDeleted(true); accountRepo.save(a); }

        List<User> contacts = userRepository.findByProviderIdStartingWith(VYP_PARTY_PREFIX);
        for (User u : contacts) {
            for (Address ad : u.getAddresses()) { ad.setDeleted(true); addressRepository.save(ad); }
            u.setDeleted(true);
            userRepository.save(u);
        }

        // Reset the opening balances this import set on the seeded accounts.
        setOpening(CASH, BigDecimal.ZERO, DrCr.DR);
        setOpening(BANK, BigDecimal.ZERO, DrCr.DR);
        setOpening(CAPITAL, BigDecimal.ZERO, DrCr.CR);
        accountRepo.findByCode(STOCK).ifPresent(a -> { a.setOpeningBalance(BigDecimal.ZERO); a.setDeleted(true); accountRepo.save(a); });

        warnings.add("Rollback removed " + products.size() + " products, " + ledgers.size()
                + " party ledgers and " + contacts.size() + " contacts; opening balances reset.");
        log.info("Vyapar import rolled back: {} products, {} ledgers, {} contacts.",
                products.size(), ledgers.size(), contacts.size());

        return new MigrationResult(false, 0, 0, products.size(), ledgers.size(),
                BigDecimal.ZERO, BigDecimal.ZERO, contacts.size(), 0,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, true, warnings);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private void applyProduct(Product p, VyaparPackage.Product vp, Map<String, Category> catCache, boolean dryRun) {
        p.setName(trunc(vp.name(), 200));
        p.setPrice(vp.price());
        p.setComparePrice(vp.comparePrice());
        p.setDescription(vp.description());
        if (p.getCategory() == null) p.setCategory(category(vp.category(), catCache, dryRun));
        if (p.getCurrency() == null) p.setCurrency("INR");
        p = productRepository.save(p);

        int qty = vp.stock() == null ? 0 : Math.max(0, vp.stock());
        int reorder = vp.reorder() == null ? 0 : Math.max(0, vp.reorder());
        Product saved = p;
        Inventory inv = inventoryRepository.findByProductId(p.getId())
                .orElseGet(() -> { Inventory i = new Inventory(); i.setProduct(saved); return i; });
        inv.setQuantity(qty);
        inv.setReorderLevel(reorder);
        inventoryRepository.save(inv);
    }

    private Category category(String name, Map<String, Category> cache, boolean dryRun) {
        String cname = (name == null || name.isBlank()) ? "General" : trunc(name, 120);
        String slug = SlugUtil.slugify(cname);
        if (slug.isBlank()) slug = "general";
        Category cached = cache.get(slug);
        if (cached != null) return cached;
        String fslug = slug, fname = cname;
        Category c = categoryRepository.findBySlug(fslug).orElseGet(() -> {
            if (dryRun) return null;
            Category n = new Category();
            n.setName(fname);
            n.setSlug(fslug);
            return categoryRepository.save(n);
        });
        if (c != null) cache.put(slug, c);
        return c;
    }

    private void upsertLedger(VyaparPackage.Party party, BigDecimal ob, boolean dr) {
        LedgerAccount a = accountRepo.findByCode(party.code()).orElseGet(LedgerAccount::new);
        a.setCode(trunc(party.code(), 40));
        a.setName(trunc(party.name(), 160));
        a.setAccountType(dr ? AccountType.ASSET : AccountType.LIABILITY);
        a.setAccountGroup(dr ? "Sundry Debtors" : "Sundry Creditors");
        a.setOpeningBalance(ob);
        a.setOpeningSide(dr ? DrCr.DR : DrCr.CR);
        a.setGstin(trunc(party.gstin(), 20));
        a.setCreditLimit(party.creditLimit());
        a.setActive(true);
        accountRepo.save(a);
    }

    private void upsertContact(VyaparPackage.Party party, User mine, String phone, String tag, Role customerRole) {
        User u = mine != null ? mine : new User();
        if (mine == null) {
            u.setProvider(AuthProvider.LOCAL);
            u.setProviderId(tag);
            u.setEmail(trunc(syntheticEmail(party), 160));      // no email in Vyapar → unique placeholder
            u.setEmailVerified(false);
            u.setEnabled(true);
            u.setWhatsappOptIn(false);                          // not opted in (Meta compliance)
            if (customerRole != null) u.getRoles().add(customerRole);
        }
        String[] parts = splitName(party.name());
        u.setFirstName(trunc(parts[0], 80));
        u.setLastName(trunc(parts[1], 80));
        u.setPhone(trunc(phone, 20));
        u = userRepository.save(u);

        String addr = trunc(party.address(), 200);
        if (addr != null && !addr.isBlank() && u.getAddresses().isEmpty()) {
            Address ad = new Address();
            ad.setUser(u);
            ad.setType(AddressType.SHIPPING);
            ad.setFullName(trunc(party.name(), 120));
            ad.setPhone(trunc(phone, 20));
            ad.setLine1(addr);
            ad.setCity("-");                                    // Vyapar has no city field
            ad.setState(trunc(party.state(), 80));
            ad.setPostalCode("-");                              // Vyapar has no pincode
            ad.setCountry("India");
            addressRepository.save(ad);
        }
    }

    /** Set the opening balance on a seeded account (creates nothing — those codes exist). */
    private void setOpening(String code, BigDecimal amount, DrCr side) {
        accountRepo.findByCode(code).ifPresent(a -> {
            a.setOpeningBalance(nz(amount));
            a.setOpeningSide(side);
            accountRepo.save(a);
        });
    }

    /** Opening Stock asset (1300) isn't seeded by default — create/update it. */
    private void ensureOpeningStock(BigDecimal stockValue) {
        LedgerAccount a = accountRepo.findByCode(STOCK).orElseGet(() -> {
            LedgerAccount n = new LedgerAccount();
            n.setCode(STOCK);
            n.setName("Opening Stock");
            n.setAccountType(AccountType.ASSET);
            n.setAccountGroup("Stock-in-Hand");
            n.setSystemAccount(true);
            return n;
        });
        a.setDeleted(false);                                    // un-delete if a prior rollback removed it
        a.setOpeningBalance(nz(stockValue));
        a.setOpeningSide(DrCr.DR);
        a.setActive(true);
        accountRepo.save(a);
    }

    private String syntheticEmail(VyaparPackage.Party party) {
        String id = party.code() == null ? "0" : party.code().replaceFirst("^VYP-P-", "");
        return "vyp-" + id + "@import.jlstores.in";
    }

    private static String[] splitName(String name) {
        String n = name == null ? "" : name.trim();
        int sp = n.indexOf(' ');
        if (sp < 0) return new String[]{n, ""};
        return new String[]{n.substring(0, sp), n.substring(sp + 1).trim()};
    }

    private static void addWarning(List<String> warnings, String msg) {
        if (warnings.size() < MAX_WARNINGS) warnings.add(msg);
        else if (warnings.size() == MAX_WARNINGS) warnings.add("… more warnings suppressed.");
    }

    private static BigDecimal nz(BigDecimal x) { return x == null ? BigDecimal.ZERO : x; }

    private static <T> List<T> nz(List<T> x) { return x == null ? List.of() : x; }

    private static String digits(String s) { return s == null ? null : s.replaceAll("[^0-9]", ""); }

    private static String last10(String digits) {
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }

    private static String trunc(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    private String uniqueSlug(String name, String sku) {
        String base = SlugUtil.slugify(name == null ? "" : name);
        if (base.isBlank()) base = SlugUtil.slugify(sku);
        if (base.isBlank() || productRepository.existsBySlug(base)) {
            base = (base.isBlank() ? "item" : base) + "-" + SlugUtil.slugify(sku);
        }
        return trunc(base, 220);
    }
}
