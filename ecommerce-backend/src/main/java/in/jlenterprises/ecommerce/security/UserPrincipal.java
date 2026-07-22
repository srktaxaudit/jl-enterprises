package in.jlenterprises.ecommerce.security;

import in.jlenterprises.ecommerce.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Adapts a {@link User} to Spring Security's {@link UserDetails}. Authorities are
 * the user's roles ({@code ROLE_*}) plus each role's permissions.
 */
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final List<GrantedAuthority> authorities;

    private UserPrincipal(UUID id, String email, String passwordHash, boolean enabled,
                          boolean accountNonLocked, List<GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
    }

    public static UserPrincipal from(User user) {
        List<GrantedAuthority> auths = new ArrayList<>();
        user.getRoles().forEach(role -> {
            auths.add(new SimpleGrantedAuthority(role.getName().name()));
            role.getPermissions().forEach(p -> auths.add(new SimpleGrantedAuthority(p.getName())));
        });
        // A failed-login lock is TEMPORARY: it holds only until lockedUntil passes, then the
        // account works again with no manual reset. A lock with no lockedUntil (set by an
        // admin) holds indefinitely. The old expression kept accountLocked=true locked
        // forever — and since a locked user can't log in to trigger the success-path reset,
        // that would have made every lockout permanent.
        boolean lockActive = user.isAccountLocked()
                && (user.getLockedUntil() == null || user.getLockedUntil().isAfter(Instant.now()));
        boolean nonLocked = !lockActive;
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(),
                user.isEnabled(), nonLocked, auths);
    }

    public UUID getId() {
        return id;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return accountNonLocked; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
