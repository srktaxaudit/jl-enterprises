package in.jlenterprises.ecommerce.dto.whatsapp;

/**
 * Meta WhatsApp Cloud API connection state for the Connection tab. Never carries the token
 * itself — only whether one is set, where it came from, and what Meta reports about the number.
 */
public record ConnectionStatusDto(
        boolean configured,        // token + phone id present (real sends possible)
        boolean tokenValid,        // Meta accepted the token on a live check
        String source,             // "portal" | "env" | "none"
        String phoneId,
        String phoneNumber,        // display_phone_number
        String verifiedName,       // verified business name
        String qualityRating,      // GREEN / YELLOW / RED / UNKNOWN
        String codeVerificationStatus,
        String wabaId,
        Integer templateCount,     // templates on the WABA (null if not checkable)
        String message
) {}
