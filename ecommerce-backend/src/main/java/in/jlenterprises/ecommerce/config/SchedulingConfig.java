package in.jlenterprises.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables Spring's {@code @Scheduled} support (used by the abandoned-order sweeper). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
