package in.jlenterprises.ecommerce.request.whatsapp;

import in.jlenterprises.ecommerce.constant.WhatsappAudienceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Create a campaign. Either compose free-form {@code bodyText} or reference a {@code templateId}. */
public record CampaignRequest(
        @NotBlank @Size(max = 160) String name,
        UUID templateId,
        @Size(max = 2000) String bodyText,
        @NotNull WhatsappAudienceType audienceType,
        @Size(max = 80) String cityFilter,
        /** Hand-picked recipient user ids — required when audienceType = MANUAL. */
        List<UUID> recipientCustomerIds
) {}
