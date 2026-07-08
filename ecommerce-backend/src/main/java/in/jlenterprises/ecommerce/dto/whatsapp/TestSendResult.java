package in.jlenterprises.ecommerce.dto.whatsapp;

/** Outcome of a single test send. */
public record TestSendResult(boolean sent, boolean demoMode, String providerMessageId, String error) {}
