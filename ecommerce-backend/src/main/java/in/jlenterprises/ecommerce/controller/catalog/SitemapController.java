package in.jlenterprises.ecommerce.controller.catalog;

import in.jlenterprises.ecommerce.config.BillingConfig;
import in.jlenterprises.ecommerce.constant.RecordStatus;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Product sitemap for search engines. Product pages are rendered client-side, so a
 * JS-less crawler sees every product as one duplicate {@code /product.html} URL —
 * this feeds them the real per-product URLs instead. Referenced (cross-host, which
 * is valid from robots.txt) by the storefront's robots.txt.
 */
@RestController
public class SitemapController {

    private static final int MAX_URLS = 5000;

    private final ProductRepository productRepository;
    private final BillingConfig billingConfig;

    public SitemapController(ProductRepository productRepository, BillingConfig billingConfig) {
        this.productRepository = productRepository;
        this.billingConfig = billingConfig;
    }

    @GetMapping(value = "/api/v1/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Transactional(readOnly = true)
    @Operation(summary = "Sitemap of live product URLs (for search engines)")
    public String sitemap() {
        // site_url is an admin Setting so a domain change never needs a code change.
        String base = billingConfig.get("site_url", "https://jlstores.in");
        StringBuilder xml = new StringBuilder(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        productRepository.findAll().stream()
                .filter(p -> p.getStatus() == RecordStatus.ACTIVE && p.getSlug() != null)
                .limit(MAX_URLS)
                .forEach(p -> {
                    xml.append("  <url><loc>").append(base).append("/product.html?slug=")
                       .append(p.getSlug()).append("</loc>");
                    if (p.getUpdatedAt() != null) {
                        xml.append("<lastmod>")
                           .append(LocalDate.ofInstant(p.getUpdatedAt(), ZoneId.of("Asia/Kolkata")))
                           .append("</lastmod>");
                    }
                    xml.append("</url>\n");
                });
        xml.append("</urlset>\n");
        return xml.toString();
    }
}
