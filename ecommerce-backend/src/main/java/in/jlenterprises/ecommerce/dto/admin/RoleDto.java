package in.jlenterprises.ecommerce.dto.admin;

import java.util.Set;
import java.util.UUID;

public record RoleDto(
        UUID id,
        String name,
        String description,
        Set<String> permissions
) {}
