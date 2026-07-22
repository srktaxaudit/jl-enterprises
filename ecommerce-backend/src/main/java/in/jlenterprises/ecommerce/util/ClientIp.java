package in.jlenterprises.ecommerce.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the real client IP behind Render's reverse proxy.
 *
 * <p>The proxy APPENDS the address it actually saw as the LAST {@code X-Forwarded-For}
 * entry; everything before it arrives from the client and is forgeable. Reading the
 * first entry lets an attacker pick their own identity per request — defeating IP rate
 * limits and polluting audit logs — so this always takes the LAST entry.
 */
public final class ClientIp {

    private ClientIp() {}

    public static String from(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.lastIndexOf(',');
            String last = (comma >= 0 ? xff.substring(comma + 1) : xff).trim();
            if (!last.isEmpty()) return last;
        }
        return request.getRemoteAddr();
    }
}
