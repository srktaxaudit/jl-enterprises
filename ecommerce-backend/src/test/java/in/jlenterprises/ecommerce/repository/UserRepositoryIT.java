package in.jlenterprises.ecommerce.repository;

import in.jlenterprises.ecommerce.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository integration test against a real PostgreSQL via Testcontainers.
 * Named *IT so it runs in the failsafe (integration-test) phase — `mvn verify`
 * — and does not slow down `mvn test`. Requires a running Docker daemon.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    UserRepository userRepository;

    @Test
    void savesAndFindsByEmailCaseInsensitively() {
        User user = new User();
        user.setEmail("shopper@example.com");
        user.setPasswordHash("hash");
        user.setFirstName("Shopper");
        userRepository.save(user);

        assertThat(userRepository.findByEmailIgnoreCase("SHOPPER@EXAMPLE.COM")).isPresent();
        assertThat(userRepository.existsByEmailIgnoreCase("shopper@example.com")).isTrue();
    }
}
