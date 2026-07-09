package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.entity.Brand;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.repository.BrandRepository;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import in.jlenterprises.ecommerce.util.SlugUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Assigns a brand to products that don't have one, inferred from the product name
 * (keyword rules tuned for the JL catalogue, incl. common Vyapar-import spellings).
 * Brands are created on demand (find-or-create by slug). Only touches products with
 * NO brand, so a manually-set brand is never overwritten. Idempotent — safe to re-run.
 *
 * This is what makes the storefront "brand" filter populate: the imported products had
 * no brand, so the dropdown only showed "All brands".
 */
@Service
public class ProductBrandAssignerService {

    private static final Logger log = LoggerFactory.getLogger(ProductBrandAssignerService.class);

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    public ProductBrandAssignerService(ProductRepository productRepository, BrandRepository brandRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
    }

    /** Returns a per-brand count of how many products were assigned. */
    @Transactional
    public Map<String, Integer> autoAssign() {
        Map<String, Brand> cache = new HashMap<>();
        Map<String, Integer> assigned = new LinkedHashMap<>();
        List<Product> changed = new ArrayList<>();

        for (Product p : productRepository.findWithoutBrand()) {
            String brandName = brandFor(p.getName());
            if (brandName == null) continue;                    // couldn't infer — leave brandless
            p.setBrand(findOrCreate(brandName, cache));
            changed.add(p);
            assigned.merge(brandName, 1, Integer::sum);
        }
        productRepository.saveAll(changed);
        log.info("Auto-brand assigned {} products: {}", changed.size(), assigned);
        return assigned;
    }

    private Brand findOrCreate(String name, Map<String, Brand> cache) {
        String slug = SlugUtil.slugify(name);
        Brand cached = cache.get(slug);
        if (cached != null) return cached;
        // Reuse any existing row with this slug — including a soft-deleted one, which we revive.
        // The slug is uniquely constrained regardless of the deleted flag, so a blind insert of
        // an already-used slug would fail the whole transaction.
        Brand brand = brandRepository.findAnyBySlug(slug).map(existing -> {
            if (existing.isDeleted()) existing.setDeleted(false);   // revive rather than clash
            return existing;
        }).orElseGet(() -> {
            Brand b = new Brand();
            b.setName(name);
            b.setSlug(slug);
            return b;
        });
        brand = brandRepository.save(brand);
        cache.put(slug, brand);
        return brand;
    }

    // ── Keyword → canonical brand (order matters: distinctive/multiword first) ──
    private String brandFor(String name) {
        if (name == null) return null;
        String n = " " + name.toLowerCase() + " ";

        if (has(n, "zebronics") || word(n, "zeb")) return "Zebronics";
        if (has(n, "blue star", "bluestar")) return "Blue Star";
        if (has(n, "daikin", "dalkin")) return "Daikin";
        if (has(n, "whirlpool")) return "Whirlpool";
        if (has(n, "panasonic")) return "Panasonic";
        if (has(n, "samsung")) return "Samsung";
        if (has(n, "voltas")) return "Voltas";
        if (has(n, "godrej")) return "Godrej";
        if (has(n, "haier")) return "Haier";
        if (has(n, "hitachi")) return "Hitachi";
        if (has(n, "toshiba")) return "Toshiba";
        if (has(n, "realme")) return "Realme";
        if (has(n, "everest")) return "Everest";
        if (has(n, "greenchef", "green chef")) return "Greenchef";
        if (has(n, "skyplus", "sky plus")) return "SkyPlus";
        if (has(n, "foxsky", "fox sky")) return "Foxsky";
        if (has(n, "iffalcon")) return "iFFALCON";
        if (has(n, "prestige")) return "Prestige";
        if (has(n, "butterfly")) return "Butterfly";
        if (has(n, "pigeon")) return "Pigeon";
        if (has(n, "havells")) return "Havells";
        if (has(n, "crompton")) return "Crompton";
        if (has(n, "bajaj")) return "Bajaj";
        if (has(n, "orient")) return "Orient";
        if (has(n, "usha")) return "Usha";
        if (has(n, "v-guard", "vguard", "v guard")) return "V-Guard";
        if (has(n, "philips")) return "Philips";
        if (has(n, "symphony")) return "Symphony";
        if (has(n, "kenstar")) return "Kenstar";
        if (has(n, "hindware")) return "Hindware";
        if (has(n, "faber")) return "Faber";
        if (has(n, "elica")) return "Elica";
        if (has(n, "ao smith", "aosmith")) return "AO Smith";
        if (has(n, "racold")) return "Racold";
        if (has(n, "lloyd")) return "Lloyd";
        if (has(n, "onida")) return "Onida";
        if (has(n, "kelvinator")) return "Kelvinator";
        if (has(n, "videocon")) return "Videocon";
        if (has(n, "boat", "boat")) return "boAt";
        if (has(n, "xiaomi", "redmi") || word(n, "mi")) return "Xiaomi";
        if (has(n, "oneplus")) return "OnePlus";
        if (has(n, "sony", "bravia")) return "Sony";
        if (word(n, "tcl")) return "TCL";
        if (word(n, "ifb")) return "IFB";
        if (word(n, "bpl")) return "BPL";
        if (word(n, "nvy")) return "NVY";
        if (word(n, "lg")) return "LG";
        if (word(n, "ae")) return "AE";

        return null;
    }

    private static boolean has(String n, String... kws) {
        for (String k : kws) if (n.contains(k)) return true;
        return false;
    }

    private static boolean word(String n, String w) {
        return Pattern.compile("\\b" + Pattern.quote(w) + "\\b").matcher(n).find();
    }
}
