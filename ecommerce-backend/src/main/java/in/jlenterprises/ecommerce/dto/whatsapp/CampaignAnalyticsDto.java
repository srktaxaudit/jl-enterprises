package in.jlenterprises.ecommerce.dto.whatsapp;

/** Overall WhatsApp marketing analytics across all campaigns. */
public record CampaignAnalyticsDto(
        long totalCampaigns,
        long totalRecipients,
        long totalSent,
        long totalDelivered,
        long totalRead,
        long totalFailed,
        double deliveryRate,
        double failRate
) {}
