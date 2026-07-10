package in.jlenterprises.ecommerce.request.whatsapp;

import jakarta.validation.constraints.Size;

/**
 * Save WhatsApp connection credentials from the portal. Every field is optional — only the
 * non-blank ones are written, so submitting a blank token leaves the existing one untouched.
 */
public record ConnectionRequest(
        @Size(max = 800) String token,
        @Size(max = 40) String phoneId,
        @Size(max = 40) String wabaId,
        @Size(max = 6) String defaultCc,
        @Size(max = 120) String verifyToken,
        @Size(max = 200) String appSecret
) {}
