package in.jlenterprises.ecommerce.util;

/**
 * Helpers for the login identifier, which may be an email OR an Indian mobile
 * number. Phone numbers are normalised to the same canonical form the signup
 * flow stores ({@code +91} + 10 digits) so a login by mobile matches the saved
 * value regardless of how the user typed it (with/without +91, spaces, etc.).
 */
public final class IdentifierUtil {

    private IdentifierUtil() {}

    /** An email identifier contains '@'; anything else is treated as a phone. */
    public static boolean isEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }

    /**
     * Canonicalise a mobile number to {@code +91XXXXXXXXXX}. Strips all non-digits,
     * drops a country-code / leading prefix beyond 10 digits, and re-adds +91.
     * If the result isn't a 10-digit number, returns the trimmed input unchanged
     * (so a bad value simply won't match any account).
     */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() > 10) digits = digits.substring(digits.length() - 10);
        return digits.length() == 10 ? "+91" + digits : raw.trim();
    }

    /**
     * The last 10 digits of a phone number, used to match an account regardless of
     * how the stored value is formatted (+91…, 91…, spaces, etc.). Returns whatever
     * digits are present if fewer than 10.
     */
    public static String last10(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("\\D", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }
}
