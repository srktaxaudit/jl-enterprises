package in.jlenterprises.ecommerce.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import in.jlenterprises.ecommerce.constant.WhatsappTemplateCategory;
import in.jlenterprises.ecommerce.dto.whatsapp.TemplateDto;
import in.jlenterprises.ecommerce.dto.whatsapp.TemplateSyncResult;
import in.jlenterprises.ecommerce.entity.WhatsappTemplate;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.notification.WhatsAppService;
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
    private final WhatsAppService whatsApp;

    public WhatsappTemplateServiceImpl(WhatsappTemplateRepository repository, WhatsAppService whatsApp) {
        this.repository = repository;
        this.whatsApp = whatsApp;
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

    @Override
    @Transactional
    public TemplateSyncResult syncFromMeta() {
        List<JsonNode> metas;
        try {
            metas = whatsApp.fetchTemplates();
        } catch (Exception e) {
            throw new BusinessException("Could not fetch templates from Meta: " + trim(e.getMessage())
                    + " — check the token and WABA ID on the Connection tab.");
        }
        if (metas.isEmpty()) {
            return new TemplateSyncResult(0, 0, 0, 0,
                    "No templates found. Set the WABA ID on the Connection tab (or create templates in Meta first).");
        }
        int imported = 0, updated = 0, skipped = 0;
        for (JsonNode m : metas) {
            String name = text(m, "name");
            if (name == null || name.isBlank()) { skipped++; continue; }
            String lang = m.path("language").asText("en");
            String status = m.path("status").asText("");
            String body = extractBody(m);

            WhatsappTemplate t = repository.findFirstByMetaTemplateNameAndLanguage(name, lang).orElse(null);
            boolean isNew = (t == null);
            if (isNew) { t = new WhatsappTemplate(); t.setName(name); }
            t.setMetaTemplateName(name);
            t.setLanguage(lang);
            t.setCategory(mapCategory(m.path("category").asText("")));
            t.setMetaStatus(status);
            if (body != null && !body.isBlank()) {
                t.setBodyText(body.length() > 2000 ? body.substring(0, 2000) : body);
            } else if (isNew) {
                t.setBodyText("(synced from Meta — this template has no body text)");
            }
            // Only Meta-approved templates can be sent live; keep others inactive.
            t.setActive("APPROVED".equalsIgnoreCase(status));
            repository.save(t);
            if (isNew) imported++; else updated++;
        }
        return new TemplateSyncResult(metas.size(), imported, updated, skipped,
                "Synced " + metas.size() + " template(s) from Meta.");
    }

    /** Body text of a Meta template's BODY component, if present. */
    private static String extractBody(JsonNode m) {
        JsonNode comps = m.path("components");
        if (comps.isArray()) {
            for (JsonNode c : comps) {
                if ("BODY".equalsIgnoreCase(c.path("type").asText(""))) {
                    String txt = c.path("text").asText(null);
                    if (txt != null && !txt.isBlank()) return txt;
                }
            }
        }
        return null;
    }

    private static WhatsappTemplateCategory mapCategory(String metaCategory) {
        if (metaCategory == null) return WhatsappTemplateCategory.MARKETING;
        String u = metaCategory.toUpperCase();
        return (u.contains("UTILITY") || u.contains("AUTH")) ? WhatsappTemplateCategory.UTILITY : WhatsappTemplateCategory.MARKETING;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    private static String trim(String s) {
        if (s == null) return "";
        return s.length() <= 300 ? s : s.substring(0, 300);
    }

    private void apply(WhatsappTemplate t, TemplateRequest r) {
        t.setName(r.name().trim());
        t.setMetaTemplateName(r.metaTemplateName() == null || r.metaTemplateName().isBlank() ? null : r.metaTemplateName().trim());
        t.setLanguage(r.language() == null || r.language().isBlank() ? "en" : r.language().trim());
        t.setCategory(r.category() == null ? WhatsappTemplateCategory.MARKETING : r.category());
        t.setBodyText(r.bodyText().trim());
    }

    private TemplateDto toDto(WhatsappTemplate t) {
        return new TemplateDto(t.getId(), t.getName(), t.getMetaTemplateName(), t.getMetaStatus(), t.getLanguage(),
                t.getCategory(), t.getBodyText(), t.getHeaderType(), t.isActive(), t.getCreatedAt());
    }
}
