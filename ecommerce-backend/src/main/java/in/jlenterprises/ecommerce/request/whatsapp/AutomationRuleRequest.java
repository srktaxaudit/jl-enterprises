package in.jlenterprises.ecommerce.request.whatsapp;

import java.util.UUID;

/** Update one event's automation rule. A null templateId clears the template (and the rule stays off live). */
public record AutomationRuleRequest(
        boolean enabled,
        UUID templateId
) {}
