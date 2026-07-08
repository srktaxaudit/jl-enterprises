package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.WhatsappAudienceType;
import in.jlenterprises.ecommerce.dto.admin.BroadcastResult;
import in.jlenterprises.ecommerce.dto.whatsapp.AudiencePreviewDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignAnalyticsDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignDetailDto;
import in.jlenterprises.ecommerce.dto.whatsapp.CampaignDto;
import in.jlenterprises.ecommerce.request.whatsapp.CampaignRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    /** Back-compat for the legacy /broadcast endpoint (and the mobile screen). */
    BroadcastResult quickBroadcast(String message, boolean onlyVerified);
}
