package in.jlenterprises.ecommerce.dto.whatsapp;

import in.jlenterprises.ecommerce.constant.WhatsappAudienceType;
import in.jlenterprises.ecommerce.constant.WhatsappCampaignStatus;

import java.time.Instant;
import java.util.UUID;

/** Campaign summary for the list view. */
public record CampaignDto(
        UUID id,
        String name,
        WhatsappAudienceType audienceType,
        WhatsappCampaignStatus campaignStatus,
        boolean demoMode,
        int totalRecipients,
        int sentCount,
        int deliveredCount,
        int readCount,
        int failedCount,
        Instant scheduledAt,
        Instant sentAt,
        Instant createdAt
) {}
