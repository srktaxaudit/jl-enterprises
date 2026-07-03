package in.jlenterprises.ecommerce.controller.customer;

import in.jlenterprises.ecommerce.dto.customer.NotificationDto;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import in.jlenterprises.ecommerce.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "In-app notifications for the current user")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List my notifications (paged, newest first)")
    public ApiResponse<PageResponse<NotificationDto>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(notificationService.list(SecurityUtils.currentUserId(), pageable)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Number of unread notifications")
    public ApiResponse<Map<String, Long>> unreadCount() {
        long count = notificationService.unreadCount(SecurityUtils.currentUserId());
        return ApiResponse.success(Map.of("unread", count));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification read")
    public ApiResponse<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(SecurityUtils.currentUserId(), id);
        return ApiResponse.message("Marked read");
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications read")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.currentUserId());
        return ApiResponse.message("All marked read");
    }
}
