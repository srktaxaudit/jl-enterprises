package in.jlenterprises.ecommerce.dto.whatsapp;

import java.util.List;

/** Recipient count + a few sample names, shown before sending. */
public record AudiencePreviewDto(int count, List<String> sampleNames) {}
