package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.dto.admin.BroadcastRequest;
import in.jlenterprises.ecommerce.dto.admin.BroadcastResult;
import in.jlenterprises.ecommerce.notification.WhatsAppService;
import in.jlenterprises.ecommerce.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/whatsapp")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER')")
@Tag(name = "Admin — WhatsApp", description = "Broadcast promotional WhatsApp messages")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    public WhatsAppController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @PostMapping("/broadcast")
    @Auditable(action = "WHATSAPP_BROADCAST", entity = "whatsapp")
    @Operation(summary = "Broadcast a message to customers with a phone number")
    public ApiResponse<BroadcastResult> broadcast(@Valid @RequestBody BroadcastRequest request) {
        BroadcastResult result = whatsAppService.broadcast(request.message(), request.onlyVerified());
        String msg = result.demoMode()
                ? "Demo mode (no WhatsApp credentials) — messages were logged, not sent."
                : "Broadcast complete.";
        return ApiResponse.success(msg, result);
    }
}
