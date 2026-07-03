package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.admin.AuditLogDto;
import in.jlenterprises.ecommerce.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface AuditLogMapper {

    AuditLogDto toDto(AuditLog auditLog);
}
