package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.entity.Category;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.repository.CategoryRepository;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Auto-files products into the right storefront department based on their name
 * (keyword rules tuned for the JL catalogue, incl. common Vyapar-import typos).
 * Only touches products that are currently in "General" or have no category, so
 * manual categorisations are never overwritten. Idempotent — safe to re-run.
 */
@Service
public class ProductCategorizerService {

    private static final Logger log = LoggerFactory.getLogger(ProductCategorizerService.class);
    private static final Set<String> SIZES = Set.of("24", "32", "40", "43", "50", "55", "65", "70", "75", "85");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductCategorizerService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /** Returns a per-category count of how many products were moved. */
    @Transactional
    public Map<String, Integer> autoCategorize() {
        Map<String, Category> bySlug = new java.util.HashMap<>();
        for (Category c : categoryRepository.findAll()) bySlug.put(c.getSlug(), c);

        Map<String, Integer> moved = new LinkedHashMap<>();
        for (Product p : productRepository.findAll()) {
            Category cur = p.getCategory();
            // Only organise uncategorised / General products — leave manual choices alone.
            if (cur != null && !"general".equalsIgnoreCase(cur.getSlug())) continue;

            String slug = categorize(p.getName());
            if (slug == null || "general".equals(slug)) continue;
            Category target = bySlug.get(slug);
            if (target == null) continue;                                   // department not created yet
            if (cur != null && cur.getId().equals(target.getId())) continue;

            p.setCategory(target);
            productRepository.save(p);
            moved.merge(slug, 1, Integer::sum);
        }
        log.info("Auto-categorize moved {} products: {}", moved.values().stream().mapToInt(Integer::intValue).sum(), moved);
        return moved;
    }

    // ── Keyword rules (order matters) ──────────────────────────────────────
    private String categorize(String name) {
        if (name == null) return null;
        String n = " " + name.toLowerCase()
                .replace("\"", " inch ").replace("{", " ").replace("}", " ").replace("[", " ").replace("]", " ") + " ";

        // Newer departments — checked first (so "table fan" isn't read as furniture, etc.)
        if (has(n, "fan")) return "fans";
        if (has(n, "cooler", "air cool")) return "air-coolers";
        if (has(n, "stabli", "stablizer", "steblizer", "stebli", "stabiliser", "steblize")) return "stabilizers";
        if (has(n, "water heater", "watter heater", "geyser")) return "water-heaters";

        // Items with no product department -> stay in General
        if (has(n, "water bott", "laptop", "thinkpad", "latitude", "inspiron", "cpu", "power bank",
                "printer", "dell ", "lenovo", "toshiba", "5g phone") || word(n, "phone")) return "general";
        if (has(n, "transpor", "installat", "fitting", "cloth change", "glass work", "tiles work",
                "painting", "celling", "ceiling decor", "name board", "electrical material",
                "led light", "cctv", "curtain", "wallmount", "wall mount") || n.trim().equals("stock")) return "general";

        // Washing machines (before AC: "Voltas semi autometic" is a washer)
        if (has(n, "washing", "washer", "mechine", "semi auto", "fully auto", "autometic", "automatic",
                "top load", "topload", "front load", "frontload", "kg", "wa80", "wa8", "wt65", "wt6",
                "drayer", "drayor")) return "washing-machines";

        // Refrigerators
        if (has(n, "fridge", "refriger", "freezer", "double door", "single door", "onion dray",
                "gl-n", "gl n", "rt28", "rt2", "rt3", "litre", "litr", "236lit") || word(n, "ref")) return "refrigerators";

        // Air conditioners
        if (has(n, "air condit", "air conditinor", " ton", "1.5ton", "1ton", "1,5 ton", "inverter ac",
                "split ac", "window ac", "ts-q", "daikin", "dalkin", "bluestar", "blue star", "carrier")
                || word(n, "ac")) return "air-conditioners";

        // Home theatre / audio
        if (has(n, "home theat", "hometheat", "homether", "soundbar", "sound bar", "speaker", "woofer",
                "amplifier", "dolby", "jukebar", "5.1", "2.1", "watts", "zeb", "zebronics")) return "home-theatre";

        // Televisions (brands, smart/android, UHD, or a size token)
        if (word(n, "tv") || has(n, "television", "android", "smart led", "smart ", "led -", "bravia",
                "nano", "uhd", "4k", "web os", "webos", "skyplus", "foxsky", "fox sky", "iffalcon", "nvy", "inch")
                || hasSize(n)) return "televisions";

        // Kitchen & Stove
        if (has(n, "mixer", "mixie", "mixey", "mixy", "grinder", "cooker", "induction", "stove", "burner",
                "kettle", "kettile", "tawa", "tiffen", "tiffin", "blender", "dosai", "cooktop", "chimney",
                "kadai", "idli", "juicer", "fryer", "chopper", "otg", "ovan", "oven", "micro", "gas ")) return "kitchen";

        // Furniture
        if (has(n, "sofa", "seater", "chair", "table", "wardrobe", "almirah", "almari", "bero", "beero",
                "beerol", "berow", "berol", "mattress", "matress", "matures", "maturess", "dressing",
                "recliner", "stool", "bench", "dining", "cupboard", "wooden", "teak", "plywood", "desk",
                "spring", "shelf", "drawer", "cabinet", "cot", "bunk", "rack", "pooja stand", "teapoy",
                "pillow", "urutt", "thoppi", "kadasal", "queen size", "full wood", "seat")
                || word(n, "bed") || word(n, "adi")) return "furniture";

        return "general";
    }

    private static boolean has(String n, String... kws) {
        for (String k : kws) if (n.contains(k)) return true;
        return false;
    }

    private static boolean word(String n, String w) {
        return Pattern.compile("\\b" + Pattern.quote(w) + "\\b").matcher(n).find();
    }

    private static boolean hasSize(String n) {
        for (String s : SIZES) if (n.contains(" " + s + " ") || n.contains(" " + s + "inch")) return true;
        return false;
    }
}
