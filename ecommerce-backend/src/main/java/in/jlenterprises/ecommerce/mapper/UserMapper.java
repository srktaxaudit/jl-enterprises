package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.auth.UserDto;
import in.jlenterprises.ecommerce.entity.Role;
import in.jlenterprises.ecommerce.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(config = CentralMapperConfig.class)
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "roleNames")
    UserDto toDto(User user);

    @Named("roleNames")
    default Set<String> roleNames(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream().map(r -> r.getName().name()).collect(Collectors.toSet());
    }
}
