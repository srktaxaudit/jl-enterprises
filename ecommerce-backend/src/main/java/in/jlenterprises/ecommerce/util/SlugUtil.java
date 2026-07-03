package in.jlenterprises.ecommerce.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Generates URL-friendly slugs from arbitrary text. */
public final class SlugUtil {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern DASHES = Pattern.compile("-{2,}");

    private SlugUtil() {}

    public static String slugify(String input) {
        if (input == null || input.isBlank()) return "";
        String noWhitespace = WHITESPACE.matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = DASHES.matcher(slug).replaceAll("-");
        return slug.toLowerCase(Locale.ENGLISH).replaceAll("^-+|-+$", "");
    }
}
