package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.admin.RoleDto;
import in.jlenterprises.ecommerce.entity.Permission;
import in.jlenterprises.ecommerce.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(config = CentralMapperConfig.class)
public interface RoleMapper {

    @Mapping(target = "permissions", source = "permissions", qualifiedByName = "permissionNames")
    RoleDto toDto(Role role);

    List<RoleDto> toDtoList(List<Role> roles);

    @Named("permissionNames")
    default Set<String> permissionNames(Set<Permission> permissions) {
        if (permissions == null) return Set.of();
        return permissions.stream().map(Permission::getName).collect(Collectors.toSet());
    }
}
