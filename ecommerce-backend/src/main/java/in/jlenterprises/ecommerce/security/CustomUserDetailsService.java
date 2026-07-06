package in.jlenterprises.ecommerce.security;

import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.util.IdentifierUtil;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a user (with roles/permissions) for authentication. The identifier may be
 * an email (used by the JWT filter, where the token subject is the email) or a
 * mobile number (login by phone) — resolved to the same account either way.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        return (IdentifierUtil.isEmail(identifier)
                ? userRepository.findByEmailIgnoreCase(identifier)
                : userRepository.findByPhone(IdentifierUtil.normalizePhone(identifier)))
                .map(UserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + identifier));
    }
}
