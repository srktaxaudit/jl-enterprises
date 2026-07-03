package in.jlenterprises.ecommerce.dto.admin;

/**
 * Outcome of a broadcast.
 * @param demoMode true when no WhatsApp credentials are configured — messages
 *                 were logged, not actually sent.
 */
public record BroadcastResult(int recipients, int sent, int failed, boolean demoMode) {}
