package in.jlenterprises.ecommerce.controller.order;

import in.jlenterprises.ecommerce.dto.order.InvoiceDto;
import in.jlenterprises.ecommerce.dto.order.OrderDto;
import in.jlenterprises.ecommerce.dto.order.OrderSummaryDto;
import in.jlenterprises.ecommerce.request.order.PlaceOrderRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import in.jlenterprises.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Checkout, order history and tracking for the current user")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place an order from the current cart")
    public ResponseEntity<ApiResponse<OrderDto>> place(@Valid @RequestBody PlaceOrderRequest request) {
        OrderDto order = orderService.placeOrder(SecurityUtils.currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Order placed", order));
    }

    @GetMapping
    @Operation(summary = "List my orders (newest first)")
    public ApiResponse<PageResponse<OrderSummaryDto>> myOrders(
            @PageableDefault(size = 20, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(orderService.myOrders(SecurityUtils.currentUserId(), pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of my orders")
    public ApiResponse<OrderDto> get(@PathVariable UUID id) {
        return ApiResponse.success(orderService.myOrder(SecurityUtils.currentUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel one of my orders")
    public ApiResponse<OrderDto> cancel(@PathVariable UUID id) {
        return ApiResponse.success("Order cancelled", orderService.cancel(SecurityUtils.currentUserId(), id));
    }

    @GetMapping("/{id}/invoice")
    @Operation(summary = "Get the invoice for one of my orders")
    public ApiResponse<InvoiceDto> invoice(@PathVariable UUID id) {
        return ApiResponse.success(orderService.invoice(SecurityUtils.currentUserId(), id));
    }
}
