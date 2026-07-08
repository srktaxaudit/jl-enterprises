package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.constant.RoleName;
import in.jlenterprises.ecommerce.dto.admin.RoleDto;
import in.jlenterprises.ecommerce.dto.auth.UserDto;
import in.jlenterprises.ecommerce.entity.Role;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.DuplicateResourceException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.RoleMapper;
import in.jlenterprises.ecommerce.mapper.UserMapper;
import in.jlenterprises.ecommerce.repository.RoleRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.request.admin.StaffRequest;
import in.jlenterprises.ecommerce.security.SecurityUtils;
import in.jlenterprises.ecommerce.service.AdminUserService;
import in.jlenterprises.ecommerce.service.RefreshTokenService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final List<RoleName> STAFF_ROLES = List.of(
            RoleName.ROLE_SUPER_ADMIN, RoleName.ROLE_ADMIN, RoleName.ROLE_MANAGER,
            RoleName.ROLE_INVENTORY_MANAGER, RoleName.ROLE_ORDER_MANAGER, RoleName.ROLE_PRODUCT_MANAGER,
            RoleName.ROLE_MARKETING_MANAGER, RoleName.ROLE_CUSTOMER_SUPPORT);

    /** Roles only a super-admin may grant or manage. Prevents an ADMIN from escalating to (or seizing) admin accounts. */
    private static final Set<RoleName> PRIVILEGED_ROLES = Set.of(RoleName.ROLE_SUPER_ADMIN, RoleName.ROLE_ADMIN);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public AdminUserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                                UserMapper userMapper, RoleMapper roleMapper, PasswordEncoder passwordEncoder,
                                RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(String search, Boolean active, Pageable pageable) {
        Specification<User> spec = buildSearch(search);
        if (active != null) {
            spec = Specification.where(spec).and((root, query, cb) -> cb.equal(root.get("enabled"), active));
        }
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
        assertCanManage(user);
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

    // ── Staff management ──
    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> listStaff(String search, Pageable pageable) {
        Specification<User> spec = staffSpec();
        Specification<User> searchSpec = buildSearch(search);
        if (searchSpec != null) spec = spec.and(searchSpec);
        return userRepository.findAll(spec, pageable).map(userMapper::toDto);
    }

    @Override
    @Transactional
    @Auditable(action = "CREATE_STAFF", entity = "user")
    public UserDto createStaff(StaffRequest r) {
        String email = r.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("A user with this email already exists");
        }
        if (r.password() == null || r.password().isBlank()) {
            throw new BusinessException("A password is required for a new staff account");
        }
        assertCanGrant(r.roles());
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(r.password()));
        u.setFirstName(r.firstName());
        u.setLastName(r.lastName());
        u.setPhone(r.phone() == null || r.phone().isBlank() ? null : r.phone().trim());
        u.setDepartment(r.department());
        u.setDesignation(r.designation());
        u.setEmailVerified(true);
        u.setEnabled(true);
        applyRoles(u, r.roles());
        return userMapper.toDto(userRepository.save(u));
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE_STAFF", entity = "user")
    public UserDto updateStaff(UUID userId, StaffRequest r) {
        User u = getEntity(userId);
        assertCanManage(u);
        assertCanGrant(r.roles());
        u.setFirstName(r.firstName());
        u.setLastName(r.lastName());
        if (r.phone() != null) u.setPhone(r.phone().isBlank() ? null : r.phone().trim());
        u.setDepartment(r.department());
        u.setDesignation(r.designation());
        if (r.roles() != null && !r.roles().isEmpty()) applyRoles(u, r.roles());
        if (r.password() != null && !r.password().isBlank()) u.setPasswordHash(passwordEncoder.encode(r.password()));
        return userMapper.toDto(userRepository.save(u));
    }

    @Override
    @Transactional
    @Auditable(action = "RESET_PASSWORD", entity = "user")
    public UserDto resetPassword(UUID userId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessException("A new password is required");
        }
        User u = getEntity(userId);
        assertCanManage(u);
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        return userMapper.toDto(userRepository.save(u));
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_STAFF", entity = "user")
    public void deleteStaff(UUID userId) {
        User u = getEntity(userId);
        assertCanManage(u);
        u.setDeleted(true);
        u.setEnabled(false);
        userRepository.save(u);
        refreshTokenService.revokeAll(u);   // a stolen refresh token must stop working once the account is removed
    }

    // ── helpers ──

    /** Only users holding at least one staff role (excludes plain customers). */
    private Specification<User> staffSpec() {
        return (root, query, cb) -> {
            query.distinct(true);
            return root.join("roles").get("name").in(STAFF_ROLES);
        };
    }

    /** A non-super-admin may not grant ROLE_ADMIN / ROLE_SUPER_ADMIN (privilege escalation). */
    private void assertCanGrant(List<RoleName> roles) {
        if (SecurityUtils.isSuperAdmin() || roles == null) return;
        if (roles.stream().anyMatch(PRIVILEGED_ROLES::contains)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Only a super-admin can grant admin roles.");
        }
    }

    /** A non-super-admin may not modify, reset, disable, or delete an account that holds an admin role. */
    private void assertCanManage(User target) {
        if (SecurityUtils.isSuperAdmin()) return;
        boolean targetIsPrivileged = target.getRoles().stream()
                .anyMatch(r -> PRIVILEGED_ROLES.contains(r.getName()));
        if (targetIsPrivileged) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Only a super-admin can manage admin accounts.");
        }
    }

    private void applyRoles(User u, List<RoleName> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BusinessException("Assign at least one role to the staff member");
        }
        Set<Role> resolved = new HashSet<>();
        for (RoleName rn : roles) {
            resolved.add(roleRepository.findByName(rn)
                    .orElseThrow(() -> ResourceNotFoundException.of("Role", rn)));
        }
        u.getRoles().clear();
        u.getRoles().addAll(resolved);
    }
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
