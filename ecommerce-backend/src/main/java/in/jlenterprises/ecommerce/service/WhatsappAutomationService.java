package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.WhatsappAutomationEvent;
import in.jlenterprises.ecommerce.dto.whatsapp.AutomationRuleDto;
import in.jlenterprises.ecommerce.entity.Order;
import in.jlenterprises.ecommerce.request.whatsapp.AutomationRuleRequest;

import java.util.List;

/** Event → WhatsApp template automation (Phase 4). */
public interface WhatsappAutomationService {

    /** Every event with its current rule state (unconfigured events appear disabled). */
    List<AutomationRuleDto> list();

    /** Enable/disable an event and/or set its template. */
    AutomationRuleDto update(WhatsappAutomationEvent event, AutomationRuleRequest request);

    /**
     * Fire an event for an order — sends the mapped template to the order's customer
     * when the rule is enabled. Best-effort: never throws, so callers' transactions
     * are never poisoned by a messaging failure.
     */
    void fire(WhatsappAutomationEvent event, Order order);
}
