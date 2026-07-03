package in.jlenterprises.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Enables {@code @Async} (used by e.g. EmailService) and {@code @Scheduled} jobs. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
