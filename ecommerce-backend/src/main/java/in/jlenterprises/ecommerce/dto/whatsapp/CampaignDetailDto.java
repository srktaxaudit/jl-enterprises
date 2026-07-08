package in.jlenterprises.ecommerce.dto.whatsapp;

import in.jlenterprises.ecommerce.constant.WhatsappAudienceType;
import in.jlenterprises.ecommerce.constant.WhatsappCampaignStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full campaign view: fields + computed rates + per-recipient logs. */
public record CampaignDetailDto(
        UUID id,
        String name,
        String bodyText,
        String templateName,
        WhatsappAudienceType audienceType,
        String cityFilter,
        WhatsappCampaignStatus campaignStatus,
        boolean demoMode,
        int totalRecipients,
        int sentCount,
        int deliveredCount,
        int readCount,
        int failedCount,
        double deliveryRate,
        double readRate,
        double failRate,
        Instant scheduledAt,
        Instant sentAt,
        Instant createdAt,
        List<MessageLogDto> logs
) {}
