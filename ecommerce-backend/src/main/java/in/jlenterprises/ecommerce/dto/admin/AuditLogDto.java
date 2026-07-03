package in.jlenterprises.ecommerce.dto.admin;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        String actor,
        String action,
        String entity,
        String entityId,
        String detail,
        String ipAddress,
        Instant createdAt
) {}
