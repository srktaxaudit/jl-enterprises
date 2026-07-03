package in.jlenterprises.ecommerce.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Records an audit entry after any {@link Auditable} method returns successfully.
 * The first UUID argument (if any) is captured as the entity id; the remaining
 * arguments form the detail.
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;

    public AuditAspect(AuditService auditService) {
        this.auditService = auditService;
    }

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void audit(JoinPoint joinPoint, Auditable auditable, Object result) {
        String entityId = firstUuid(joinPoint.getArgs());
        String detail = Arrays.stream(joinPoint.getArgs())
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        auditService.record(auditable.action(), auditable.entity(), entityId, detail);
    }

    private String firstUuid(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof UUID uuid) return uuid.toString();
        }
        return null;
    }
}
