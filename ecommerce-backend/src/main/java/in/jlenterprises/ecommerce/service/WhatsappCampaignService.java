package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.WhatsappAudienceType;
import in.jlenterprises.ecommerce.constant.WhatsappMessageStatus;
import in.jlenterprises.ecommerce.dto.admin.BroadcastResult;
import in.jlenterprises.ecommerce.dto.whatsapp.AudienceCustomerDto;
import in.jlenterprises.ecommerce.dto.whatsapp.AudiencePreviewDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignAnalyticsDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignDetailDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignDto;
import in.jlenterprises.ecommerce.dto.whatsapp.DeliveryLogDto;
import in.jlenterprises.ecommerce.dto.whatsapp.TestSendResult;
import in.jlenterprises.ecommerce.request.whatsapp.CampaignRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface WhatsappCampaignService {
    AudiencePreviewDto previewAudience(WhatsappAudienceType audienceType, String city);
    Page<CampaignDto> list(Pageable pageable);
    CampaignDetailDto get(UUID id);
    CampaignDetailDto create(CampaignRequest request);
    CampaignDetailDto send(UUID id);
    CampaignDetailDto retryFailed(UUID id);
    CampaignDetailDto cancel(UUID id);
    void delete(UUID id);
    CampaignAnalyticsDto analytics();
    /** Send one message to a single number to verify setup before broadcasting. */
    TestSendResult testSend(String phone, UUID templateId, String bodyText);
    /** Back-compat for the legacy /broadcast endpoint (and the mobile screen). */
    BroadcastResult quickBroadcast(String message, boolean onlyVerified);

    /** Recompute a campaign's sent/delivered/read/failed counts from its logs (webhook updates). */
    void recomputeCounts(UUID campaignId);

    /** Filtered, paged global delivery log. */
    Page<DeliveryLogDto> deliveryLog(WhatsappMessageStatus status, UUID campaignId, String phone,
                                     Instant from, Instant to, Pageable pageable);

    /** Filtered, paged customers for the broadcast audience picker (Phase 3). */
    Page<AudienceCustomerDto> audienceCustomers(UUID categoryId, String city, Boolean optedIn,
                                                Boolean phoneVerified, Boolean ordered, Boolean emi,
                                                String search, Pageable pageable);

    /** Distinct saved cities for the picker's city dropdown. */
    java.util.List<String> audienceCities();
}
