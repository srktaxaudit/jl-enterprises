package in.jlenterprises.ecommerce.dto.auth;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Safe representation of a user returned to clients (never includes the hash). */
public record UserDto(
        UUID id,
        String email,
        String phone,
        String firstName,
        String lastName,
        String department,
        String designation,
        boolean enabled,
        boolean emailVerified,
        boolean phoneVerified,
        boolean whatsappOptIn,
        Instant lastLoginAt,
        Set<String> roles
) {}
