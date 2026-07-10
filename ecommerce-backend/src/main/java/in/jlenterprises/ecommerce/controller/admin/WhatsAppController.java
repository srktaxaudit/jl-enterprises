package in.jlenterprises.ecommerce.controller.admin;

import in.jlenterprises.ecommerce.constant.WhatsappAudienceType;
import in.jlenterprises.ecommerce.dto.admin.BroadcastRequest;
import in.jlenterprises.ecommerce.dto.admin.BroadcastResult;
import in.jlenterprises.ecommerce.dto.whatsapp.AudiencePreviewDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignAnalyticsDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignDetailDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignDto;
import in.jlenterprises.ecommerce.dto.whatsapp.ConnectionStatusDto;
import in.jlenterprises.ecommerce.dto.whatsapp.TemplateDto;
import in.jlenterprises.ecommerce.dto.whatsapp.TemplateSyncResult;
import in.jlenterprises.ecommerce.dto.whatsapp.TestSendResult;
import in.jlenterprises.ecommerce.request.whatsapp.CampaignRequest;
import in.jlenterprises.ecommerce.request.whatsapp.ConnectionRequest;
import in.jlenterprises.ecommerce.request.whatsapp.TemplateRequest;
import in.jlenterprises.ecommerce.request.whatsapp.TestSendRequest;
import in.jlenterprises.ecommerce.response.ApiResponse;
import in.jlenterprises.ecommerce.response.PageResponse;
import in.jlenterprises.ecommerce.service.WhatsappCampaignService;
import in.jlenterprises.ecommerce.service.WhatsappConnectionService;
import in.jlenterprises.ecommerce.service.WhatsappTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** WhatsApp marketing — templates, campaigns, audience, analytics (staff, marketing). */
@RestController
@RequestMapping("/api/v1/admin/whatsapp")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','MANAGER','MARKETING_MANAGER')")
@Tag(name = "Admin — WhatsApp Marketing", description = "Compose, send and track WhatsApp campaigns")
public class WhatsAppController {

    private final WhatsappCampaignService campaigns;
    private final WhatsappTemplateService templates;
    private final WhatsappConnectionService connection;

    public WhatsAppController(WhatsappCampaignService campaigns, WhatsappTemplateService templates,
                             WhatsappConnectionService connection) {
        this.campaigns = campaigns;
        this.templates = templates;
        this.connection = connection;
    }

    // ── Connection ──
    @GetMapping("/connection")
    @Operation(summary = "Meta connection status (token validity, phone quality, template count)")
    public ApiResponse<ConnectionStatusDto> connection() {
        return ApiResponse.success(connection.status());
    }

    @PostMapping("/connection")
    @Operation(summary = "Save connection credentials (token/phone id/WABA/verify token)")
    public ApiResponse<ConnectionStatusDto> saveConnection(@Valid @RequestBody ConnectionRequest request) {
        return ApiResponse.success("Connection saved", connection.save(request));
    }

    @PostMapping("/connection/detect-waba")
    @Operation(summary = "Auto-detect and save the WhatsApp Business Account id from the token")
    public ApiResponse<ConnectionStatusDto> detectWaba() {
        return ApiResponse.success("WABA detected", connection.detectWaba());
    }

    // ── Templates ──
    @GetMapping("/templates")
    @Operation(summary = "List saved message templates")
    public ApiResponse<List<TemplateDto>> listTemplates() {
        return ApiResponse.success(templates.list());
    }

    @PostMapping("/templates/sync")
    @Operation(summary = "Sync approved templates from Meta into the local list")
    public ApiResponse<TemplateSyncResult> syncTemplates() {
        return ApiResponse.success("Templates synced", templates.syncFromMeta());
    }

    @PostMapping("/templates")
    @Operation(summary = "Create a template")
    public ResponseEntity<ApiResponse<TemplateDto>> createTemplate(@Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Template saved", templates.create(request)));
    }

    @PutMapping("/templates/{id}")
    @Operation(summary = "Update a template")
    public ApiResponse<TemplateDto> updateTemplate(@PathVariable UUID id, @Valid @RequestBody TemplateRequest request) {
        return ApiResponse.success("Template updated", templates.update(id, request));
    }

    @DeleteMapping("/templates/{id}")
    @Operation(summary = "Delete a template")
    public ApiResponse<Void> deleteTemplate(@PathVariable UUID id) {
        templates.delete(id);
        return ApiResponse.message("Template deleted");
    }

    // ── Campaigns ──
    @GetMapping("/campaigns")
    @Operation(summary = "List campaigns (newest first)")
    public ApiResponse<PageResponse<CampaignDto>> listCampaigns(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(campaigns.list(pageable)));
    }

    @GetMapping("/campaigns/{id}")
    @Operation(summary = "Get one campaign with logs + analytics")
    public ApiResponse<CampaignDetailDto> getCampaign(@PathVariable UUID id) {
        return ApiResponse.success(campaigns.get(id));
    }

    @PostMapping("/campaigns")
    @Operation(summary = "Create a draft campaign")
    public ResponseEntity<ApiResponse<CampaignDetailDto>> createCampaign(@Valid @RequestBody CampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Campaign created", campaigns.create(request)));
    }

    @PostMapping("/campaigns/{id}/send")
    @Operation(summary = "Send a campaign now")
    public ApiResponse<CampaignDetailDto> sendCampaign(@PathVariable UUID id) {
        return ApiResponse.success("Campaign sent", campaigns.send(id));
    }

    @PostMapping("/campaigns/{id}/retry-failed")
    @Operation(summary = "Retry the failed messages of a campaign")
    public ApiResponse<CampaignDetailDto> retryFailed(@PathVariable UUID id) {
        return ApiResponse.success("Retried failed messages", campaigns.retryFailed(id));
    }

    @PostMapping("/campaigns/{id}/cancel")
    @Operation(summary = "Cancel a campaign")
    public ApiResponse<CampaignDetailDto> cancelCampaign(@PathVariable UUID id) {
        return ApiResponse.success("Campaign cancelled", campaigns.cancel(id));
    }

    @DeleteMapping("/campaigns/{id}")
    @Operation(summary = "Delete a campaign")
    public ApiResponse<Void> deleteCampaign(@PathVariable UUID id) {
        campaigns.delete(id);
        return ApiResponse.message("Campaign deleted");
    }

    // ── Audience + analytics ──
    @GetMapping("/audience/preview")
    @Operation(summary = "Recipient count + sample names for an audience")
    public ApiResponse<AudiencePreviewDto> audiencePreview(@RequestParam WhatsappAudienceType type,
                                                           @RequestParam(required = false) String city) {
        return ApiResponse.success(campaigns.previewAudience(type, city));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Overall WhatsApp marketing analytics")
    public ApiResponse<CampaignAnalyticsDto> analytics() {
        return ApiResponse.success(campaigns.analytics());
    }

    @PostMapping("/test-send")
    @Operation(summary = "Send one test message to a number (verify setup before broadcasting)")
    public ApiResponse<TestSendResult> testSend(@Valid @RequestBody TestSendRequest request) {
        return ApiResponse.success("Test send attempted", campaigns.testSend(request.phone(), request.templateId(), request.bodyText()));
    }

    // ── Legacy quick broadcast (kept for the mobile app + back-compat) ──
    @PostMapping("/broadcast")
    @Operation(summary = "Quick broadcast a message to customers with a phone number")
    public ApiResponse<BroadcastResult> broadcast(@Valid @RequestBody BroadcastRequest request) {
        BroadcastResult result = campaigns.quickBroadcast(request.message(), request.onlyVerified());
        String msg = result.demoMode()
                ? "Demo mode (no WhatsApp credentials) — messages were logged, not sent."
                : "Broadcast complete.";
        return ApiResponse.success(msg, result);
    }
}
