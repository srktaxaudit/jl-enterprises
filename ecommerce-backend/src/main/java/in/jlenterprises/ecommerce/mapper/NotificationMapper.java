package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.customer.NotificationDto;
import in.jlenterprises.ecommerce.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface NotificationMapper {

    NotificationDto toDto(Notification notification);
}
