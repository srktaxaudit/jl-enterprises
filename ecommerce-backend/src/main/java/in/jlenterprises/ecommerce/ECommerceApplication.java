package in.jlenterprises.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * JL Enterprises — E-Commerce backend entry point.
 *
 * A modular-monolith Spring Boot 3 service: cross-cutting concerns live in
 * technical packages (config, security, exception, ...) and business logic is
 * grouped by domain (product, order, cart, ...) within the service/repository
 * layers. Cross-cutting features (JPA auditing, caching, async, scheduling) are
 * switched on via dedicated @Configuration classes in the {@code config}
 * package rather than here, to keep this class minimal and testable.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ECommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ECommerceApplication.class, args);
    }
}
