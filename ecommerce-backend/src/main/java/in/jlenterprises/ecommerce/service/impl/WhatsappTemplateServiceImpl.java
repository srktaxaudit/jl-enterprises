package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.WhatsappTemplateCategory;
import in.jlenterprises.ecommerce.dto.whatsapp.TemplateDto;
import in.jlenterprises.ecommerce.entity.WhatsappTemplate;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.WhatsappTemplateRepository;
import in.jlenterprises.ecommerce.request.whatsapp.TemplateRequest;
import in.jlenterprises.ecommerce.service.WhatsappTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class WhatsappTemplateServiceImpl implements WhatsappTemplateService {

    private final WhatsappTemplateRepository repository;

    public WhatsappTemplateServiceImpl(WhatsappTemplateRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateDto> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateDto> activeList() {
        return repository.findByActiveTrueOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public TemplateDto create(TemplateRequest r) {
        WhatsappTemplate t = new WhatsappTemplate();
        apply(t, r);
        return toDto(repository.save(t));
    }

    @Override
    @Transactional
    public TemplateDto update(UUID id, TemplateRequest r) {
        WhatsappTemplate t = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("WhatsappTemplate", id));
        apply(t, r);
        return toDto(repository.save(t));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        WhatsappTemplate t = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("WhatsappTemplate", id));
        t.setDeleted(true);
        repository.save(t);
    }

    private void apply(WhatsappTemplate t, TemplateRequest r) {
        t.setName(r.name().trim());
        t.setMetaTemplateName(r.metaTemplateName() == null || r.metaTemplateName().isBlank() ? null : r.metaTemplateName().trim());
        t.setLanguage(r.language() == null || r.language().isBlank() ? "en" : r.language().trim());
        t.setCategory(r.category() == null ? WhatsappTemplateCategory.MARKETING : r.category());
        t.setBodyText(r.bodyText().trim());
    }

    private TemplateDto toDto(WhatsappTemplate t) {
        return new TemplateDto(t.getId(), t.getName(), t.getMetaTemplateName(), t.getLanguage(),
                t.getCategory(), t.getBodyText(), t.getHeaderType(), t.isActive(), t.getCreatedAt());
    }
}
