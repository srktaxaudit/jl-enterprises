package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.dto.admin.AuditLogDto;
import in.jlenterprises.ecommerce.mapper.AuditLogMapper;
import in.jlenterprises.ecommerce.repository.AuditLogRepository;
import in.jlenterprises.ecommerce.service.AuditLogQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogQueryServiceImpl implements AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogQueryServiceImpl(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> list(Pageable pageable) {
        return auditLogRepository.findAll(pageable).map(auditLogMapper::toDto);
    }
}
