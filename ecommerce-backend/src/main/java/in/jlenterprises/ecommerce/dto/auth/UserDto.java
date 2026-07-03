package in.jlenterprises.ecommerce.dto.auth;

import java.util.Set;
import java.util.UUID;

/** Safe representation of a user returned to clients (never includes the hash). */
public record UserDto(
        UUID id,
        String email,
        String phone,
        String firstName,
        String lastName,
        boolean emailVerified,
        boolean phoneVerified,
        Set<String> roles
) {}
