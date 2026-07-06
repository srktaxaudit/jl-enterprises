package in.jlenterprises.ecommerce.config;

import in.jlenterprises.ecommerce.service.AccountingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Seeds the default chart of accounts on startup (best-effort; runs after role seeding). */
@Component
@Order(20)
public class AccountingSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountingSeeder.class);

    private final AccountingService accountingService;

    public AccountingSeeder(AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    @Override
    public void run(String... args) {
        try {
            accountingService.ensureDefaultAccounts();
        } catch (Exception e) {
            log.warn("Default chart-of-accounts seeding skipped: {}", e.toString());
        }
    }
}
