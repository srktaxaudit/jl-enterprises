package in.jlenterprises.ecommerce.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Generates URL-friendly slugs from arbitrary text. */
public final class SlugUtil {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    private SlugUtil() {}

    public static String slugify(String input) {
        if (input == null || input.isBlank()) return "";
        // Strip accents (NFD + drop combining marks), then turn every run of
        // non-alphanumeric characters into a single hyphen. This keeps meaningful
        // separators — e.g. "1.5 Ton" -> "1-5-ton" (not "15-ton").
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        String noMarks = COMBINING_MARKS.matcher(normalized).replaceAll("");
        String slug = NON_ALNUM.matcher(noMarks.toLowerCase(Locale.ENGLISH)).replaceAll("-");
        return EDGE_DASHES.matcher(slug).replaceAll("");
    }
}
