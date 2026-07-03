package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.admin.AuditLogDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Read-side access to the audit trail (writing is handled in the audit module, Step 20). */
public interface AuditLogQueryService {

    Page<AuditLogDto> list(Pageable pageable);
}
