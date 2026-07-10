package in.jlenterprises.ecommerce.request.whatsapp;

import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Reply in an Inbox conversation. Free {@code text} only works inside the 24h
 * customer-service window; outside it a {@code templateId} must be used instead.
 */
public record InboxReplyRequest(
        @Size(max = 2000) String text,
        UUID templateId
) {}
