package in.jlenterprises.ecommerce.controller.payment;

import in.jlenterprises.ecommerce.dto.order.OrderPaymentDto;
import in.jlenterprises.ecommerce.dto.payment.PaymentInitResponse;
import in.jlenterprises.ecommerce.request.payment.PaymentConfirmRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import in.jlenterprises.ecommerce.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Initiate, confirm and refund payments (Strategy-based providers)")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{orderId}/initiate")
    @Operation(summary = "Initiate payment for one of my orders")
    public ApiResponse<PaymentInitResponse> initiate(@PathVariable UUID orderId) {
        return ApiResponse.success(paymentService.initiate(SecurityUtils.currentUserId(), orderId));
    }

    @PostMapping("/{orderId}/confirm")
    @Operation(summary = "Confirm a payment after the provider callback")
    public ApiResponse<OrderPaymentDto> confirm(@PathVariable UUID orderId,
                                                @Valid @RequestBody PaymentConfirmRequest request) {
        return ApiResponse.success("Payment processed",
                paymentService.confirm(SecurityUtils.currentUserId(), orderId, request));
    }

    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @Operation(summary = "Refund an order's payment (admin)")
    public ApiResponse<OrderPaymentDto> refund(@PathVariable UUID orderId) {
        return ApiResponse.success("Refund issued", paymentService.refund(orderId));
    }
}
