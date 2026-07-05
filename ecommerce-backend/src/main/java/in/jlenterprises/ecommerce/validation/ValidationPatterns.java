package in.jlenterprises.ecommerce.validation;

/**
 * Shared regex constants for {@code @Pattern} constraints so input rules stay
 * consistent across every request DTO (and mirror the frontend jl-ui.js rules).
 * Unicode-aware ({@code \p{L}} letters, {@code \p{M}} marks) so non-English names
 * are accepted. All are compile-time constants (usable in annotations).
 */
public final class ValidationPatterns {

    private ValidationPatterns() {}

    /** Person / place names: letters, spaces and . ' - only (must start with a letter). */
    public static final String NAME = "^[\\p{L}\\p{M}][\\p{L}\\p{M}\\s.'-]*$";
    /** Optional name (empty allowed). */
    public static final String NAME_OPT = "^$|" + NAME;

    /** Brand / category-style titles: letters, digits, spaces and & . ' - (e.g. "3M", "V-Guard"). */
    public static final String TITLE = "^[\\p{L}\\p{M}\\p{N}][\\p{L}\\p{M}\\p{N}\\s.&'-]*$";
    /** Optional title (empty allowed). */
    public static final String TITLE_OPT = "^$|" + TITLE;

    /** Coupon-style code: letters and digits only. */
    public static final String CODE = "^[A-Za-z0-9]+$";

    /** Phone: optional +, then 7–14 digits. */
    public static final String PHONE = "^[0-9+][0-9]{7,14}$";
    /** Optional phone (empty allowed). */
    public static final String PHONE_OPT = "^$|" + PHONE;

    /** Postal code: 4–10 digits. */
    public static final String POSTAL = "^\\d{4,10}$";

    // Human-friendly messages
    public static final String MSG_NAME = "Only letters and spaces are allowed";
    public static final String MSG_TITLE = "Use only letters, numbers and spaces";
    public static final String MSG_CODE = "Use only letters and numbers";
    public static final String MSG_PHONE = "Enter a valid phone number";
    public static final String MSG_POSTAL = "Enter a valid postal code";
}
