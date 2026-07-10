package in.jlenterprises.ecommerce.dto.whatsapp;

import in.jlenterprises.ecommerce.constant.WhatsappAutomationEvent;

import java.util.UUID;

/** One automation event's rule state for the Automation tab (every event is always listed). */
public record AutomationRuleDto(
        WhatsappAutomationEvent event,
        boolean enabled,
        UUID templateId,
        String templateName,
        String templateMetaStatus,
        int sentCount,
        int failedCount
) {}
