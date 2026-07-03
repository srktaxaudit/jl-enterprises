package in.jlenterprises.ecommerce.security;

import in.jlenterprises.ecommerce.exception.InvalidTokenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

/** Helpers to read the current authenticated principal from the SecurityContext. */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<UserPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /** @throws InvalidTokenException if there is no authenticated user. */
    public static UUID currentUserId() {
        return currentPrincipal()
                .map(UserPrincipal::getId)
                .orElseThrow(() -> new InvalidTokenException("No authenticated user"));
    }

    public static String currentEmail() {
        return currentPrincipal()
                .map(UserPrincipal::getUsername)
                .orElseThrow(() -> new InvalidTokenException("No authenticated user"));
    }
}
