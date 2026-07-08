package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.whatsapp.TemplateDto;
import in.jlenterprises.ecommerce.request.whatsapp.TemplateRequest;

import java.util.List;
import java.util.UUID;

public interface WhatsappTemplateService {
    List<TemplateDto> list();
    List<TemplateDto> activeList();
    TemplateDto create(TemplateRequest request);
    TemplateDto update(UUID id, TemplateRequest request);
    void delete(UUID id);
}
