package in.jlenterprises.ecommerce.dto.whatsapp;

import in.jlenterprises.ecommerce.constant.WhatsappTemplateCategory;

import java.time.Instant;
import java.util.UUID;

public record TemplateDto(
        UUID id,
        String name,
        String metaTemplateName,
        String metaStatus,
        String language,
        WhatsappTemplateCategory category,
        String bodyText,
        String headerType,
        boolean active,
        Instant createdAt
) {}
