package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.dto.order.OrderDto;
import in.jlenterprises.ecommerce.dto.order.OrderSummaryDto;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER')")
@Tag(name = "Admin — Orders", description = "Order management and fulfilment (staff)")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List all orders, optionally filtered by status")
    public ApiResponse<PageResponse<OrderSummaryDto>> list(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(orderService.listAll(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get any order by id")
    public ApiResponse<OrderDto> get(@PathVariable UUID id) {
        return ApiResponse.success(orderService.adminGet(id));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List a specific customer's orders")
    public ApiResponse<PageResponse<OrderSummaryDto>> byUser(
            @PathVariable UUID userId,
            @PageableDefault(size = 20, sort = "placedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(orderService.userOrders(userId, pageable)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Advance an order's status (validates transitions; auto-restocks on cancel/return)")
    public ApiResponse<OrderDto> updateStatus(@PathVariable UUID id, @RequestParam OrderStatus status) {
        return ApiResponse.success("Order status updated", orderService.updateStatus(id, status));
    }

    @PostMapping("/{id}/return/approve")
    @Operation(summary = "Approve a return request (restock + refund)")
    public ApiResponse<OrderDto> approveReturn(@PathVariable UUID id) {
        return ApiResponse.success("Return approved", orderService.approveReturn(id));
    }

    @PostMapping("/{id}/return/reject")
    @Operation(summary = "Reject a return request with a reason")
    public ApiResponse<OrderDto> rejectReturn(@PathVariable UUID id, @RequestParam(required = false) String reason) {
        return ApiResponse.success("Return rejected", orderService.rejectReturn(id, reason));
    }

    @PatchMapping("/{id}/notes")
    @Operation(summary = "Set internal (staff-only) notes on an order")
    public ApiResponse<OrderDto> setNotes(@PathVariable UUID id, @RequestParam(required = false) String notes) {
        return ApiResponse.success("Notes saved", orderService.setAdminNotes(id, notes));
    }
}
