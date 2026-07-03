package in.jlenterprises.ecommerce.config;

import in.jlenterprises.ecommerce.constant.RoleName;
import in.jlenterprises.ecommerce.entity.Role;
import in.jlenterprises.ecommerce.entity.User;
import in.jlenterprises.ecommerce.repository.RoleRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the fixed roles on startup and, if absent, a bootstrap SUPER_ADMIN so
 * the system is usable on a fresh database. The admin credentials come from env
 * (with dev defaults) — change them in any real environment.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.bootstrap.admin-email:admin@jlenterprises.in}") String adminEmail,
                           @Value("${app.bootstrap.admin-password:Admin@12345}") String adminPassword) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (RoleName name : RoleName.values()) {
            if (!roleRepository.existsByName(name)) {
                Role role = new Role();
                role.setName(name);
                role.setDescription(name.name());
                roleRepository.save(role);
                log.info("Seeded role {}", name);
            }
        }

        if (!userRepository.existsByEmailIgnoreCase(adminEmail)) {
            Role superAdmin = roleRepository.findByName(RoleName.ROLE_SUPER_ADMIN).orElseThrow();
            User admin = new User();
            admin.setEmail(adminEmail.toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setFirstName("Super");
            admin.setLastName("Admin");
            admin.setEmailVerified(true);
            admin.getRoles().add(superAdmin);
            userRepository.save(admin);
            log.warn("Seeded SUPER_ADMIN account '{}'. CHANGE THE DEFAULT PASSWORD immediately.", adminEmail);
        }
    }
}
