package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.customer.AddressDto;
import in.jlenterprises.ecommerce.entity.Address;
import in.jlenterprises.ecommerce.request.customer.AddressRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface AddressMapper {

    AddressDto toDto(Address address);

    List<AddressDto> toDtoList(List<Address> addresses);

    @Mapping(target = "user", ignore = true)
    Address toEntity(AddressRequest request);

    @Mapping(target = "user", ignore = true)
    void updateEntity(@MappingTarget Address address, AddressRequest request);
}
