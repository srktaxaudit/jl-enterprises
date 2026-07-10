package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.order.OrderDto;
import in.jlenterprises.ecommerce.request.admin.PosOrderRequest;

public interface PosOrderService {

    /** Record a completed counter/phone sale: deduct stock, mark cash-paid, post to the books. */
    OrderDto createPosOrder(PosOrderRequest request);
}
