package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.order.OrderPaymentDto;
import in.jlenterprises.ecommerce.dto.payment.PaymentInitResponse;
import in.jlenterprises.ecommerce.request.payment.PaymentConfirmRequest;

import java.util.UUID;

public interface PaymentService {

    PaymentInitResponse initiate(UUID userId, UUID orderId);

    OrderPaymentDto confirm(UUID userId, UUID orderId, PaymentConfirmRequest request);

    OrderPaymentDto refund(UUID orderId);
}
