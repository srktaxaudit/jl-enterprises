package in.jlenterprises.ecommerce.security.jwt;

import in.jlenterprises.ecommerce.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService newService() {
        var jwt = new AppProperties.Jwt(
                "test-secret-that-is-at-least-32-bytes-long!!",
                Duration.ofMinutes(15), Duration.ofDays(7), Duration.ofDays(30), "jl-test");
        return new JwtService(new AppProperties(jwt, null, null, null, null, null));
    }

    @Test
    void issuesAndValidatesToken() {
        JwtService service = newService();
        String token = service.generateAccessToken("user@example.com", List.of("ROLE_CUSTOMER"));

        assertTrue(service.isValid(token));
        assertEquals("user@example.com", service.extractSubject(token));
    }

    @Test
    void rejectsGarbageToken() {
        assertFalse(newService().isValid("not-a-real-jwt"));
    }
}
