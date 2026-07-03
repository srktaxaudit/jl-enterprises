package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.RoleName;
import in.jlenterprises.ecommerce.dto.admin.RoleDto;
import in.jlenterprises.ecommerce.dto.auth.UserDto;
import in.jlenterprises.ecommerce.entity.Role;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.RoleMapper;
import in.jlenterprises.ecommerce.mapper.UserMapper;
import in.jlenterprises.ecommerce.repository.RoleRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;

    public AdminUserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                                UserMapper userMapper, RoleMapper roleMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(String search, Pageable pageable) {
        Specification<User> spec = buildSearch(search);
        return userRepository.findAll(spec, pageable).map(userMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(UUID userId) {
        return userMapper.toDto(getEntity(userId));
    }

    @Override
    @Transactional
    @Auditable(action = "SET_USER_ENABLED", entity = "user")
    public UserDto setEnabled(UUID userId, boolean enabled) {
        User user = getEntity(userId);
        user.setEnabled(enabled);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    @Auditable(action = "ASSIGN_ROLE", entity = "user")
    public UserDto assignRole(UUID userId, RoleName roleName) {
        User user = getEntity(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", roleName));
        user.getRoles().add(role);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDto removeRole(UUID userId, RoleName roleName) {
        User user = getEntity(userId);
        if (user.getRoles().size() <= 1) {
            throw new BusinessException("A user must retain at least one role");
        }
        user.getRoles().removeIf(r -> r.getName() == roleName);
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        return roleMapper.toDtoList(roleRepository.findAll());
    }

    // ── helpers ──
    private User getEntity(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    /** Case-insensitive match on email / first / last name. Null when no search term. */
    private Specification<User> buildSearch(String search) {
        if (search == null || search.isBlank()) return null;
        String like = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("email")), like),
                cb.like(cb.lower(root.get("firstName")), like),
                cb.like(cb.lower(root.get("lastName")), like));
    }
}
