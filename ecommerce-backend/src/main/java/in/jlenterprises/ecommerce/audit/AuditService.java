package in.jlenterprises.ecommerce.audit;

import in.jlenterprises.ecommerce.entity.AuditLog;
import in.jlenterprises.ecommerce.repository.AuditLogRepository;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Persists audit-trail entries. Failures here never disrupt the audited operation. */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final int MAX_DETAIL = 2000;

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** Written in its own transaction so an audit failure can't roll back business work. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entity, String entityId, String detail) {
        try {
            AuditLog entry = new AuditLog();
            entry.setActor(currentActor());
            entry.setAction(action);
            entry.setEntity(entity);
            entry.setEntityId(entityId);
            entry.setDetail(truncate(detail));
            HttpServletRequest request = currentRequest();
            if (request != null) {
                entry.setIpAddress(clientIp(request));
                entry.setUserAgent(request.getHeader("User-Agent"));
            }
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to write audit entry action={} entity={}", action, entity, e);
        }
    }

    private String currentActor() {
        return SecurityUtils.currentPrincipal()
                .map(p -> p.getUsername())
                .orElse("system");
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        return (attrs instanceof ServletRequestAttributes sra) ? sra.getRequest() : null;
    }

    private String clientIp(HttpServletRequest request) {
        return in.jlenterprises.ecommerce.util.ClientIp.from(request);
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() <= MAX_DETAIL ? s : s.substring(0, MAX_DETAIL);
    }
}
