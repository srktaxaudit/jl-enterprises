package in.jlenterprises.ecommerce.request.whatsapp;

import in.jlenterprises.ecommerce.constant.WhatsappTemplateCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemplateRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 160) String metaTemplateName,
        @Size(max = 10) String language,
        WhatsappTemplateCategory category,
        @NotBlank @Size(max = 2000) String bodyText
) {}
