package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.RoleName;
import in.jlenterprises.ecommerce.dto.admin.RoleDto;
import in.jlenterprises.ecommerce.dto.auth.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Admin operations over users and roles. */
public interface AdminUserService {

    Page<UserDto> listUsers(String search, Pageable pageable);

    UserDto getUser(UUID userId);

    UserDto setEnabled(UUID userId, boolean enabled);

    UserDto assignRole(UUID userId, RoleName role);

    UserDto removeRole(UUID userId, RoleName role);

    List<RoleDto> listRoles();
}
