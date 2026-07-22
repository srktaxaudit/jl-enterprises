package in.jlenterprises.ecommerce.config;

import in.jlenterprises.ecommerce.entity.AppSetting;
import in.jlenterprises.ecommerce.repository.AppSettingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Store billing/tax configuration, read from the admin Settings (key/value) with
 * sensible defaults. Edit these on the admin Settings page:
 *   gst_rate (e.g. 18) · seller_gstin · seller_name · seller_address
 */
@Component
public class BillingConfig {

    private final AppSettingRepository settings;

    public BillingConfig(AppSettingRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public String get(String key, String defaultValue) {
        return settings.findById(key)
                .map(AppSetting::getValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);
    }

    /** GST rate as a percentage (default 18). Falls back to 18 if misconfigured. */
    public BigDecimal gstRate() {
        try {
            return new BigDecimal(get("gst_rate", "18").trim());
        } catch (NumberFormatException e) {
            return new BigDecimal("18");
        }
    }

    public String sellerGstin() {
        return get("seller_gstin", "");
    }

    public String sellerName() {
        return get("seller_name", "JL Enterprises");
    }

    public String sellerAddress() {
        return get("seller_address", "185G/1B, Palai Road, Chidambaramnagar, Thoothukudi, Tamil Nadu 628008");
    }

    /** Seller's GST state — decides CGST/SGST (intra-state) vs IGST (inter-state) on invoices. */
    public String sellerState() {
        return get("seller_state", "Tamil Nadu");
    }
}
