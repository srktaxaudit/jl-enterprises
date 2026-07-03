package in.jlenterprises.ecommerce.util;

import in.jlenterprises.ecommerce.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Produces human-friendly, unique order numbers: {@code JL} + 6 digits. */
@Component
public class OrderNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;

    public OrderNumberGenerator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public String generate() {
        String candidate;
        do {
            candidate = "JL" + (100000 + RANDOM.nextInt(900000));
        } while (orderRepository.findByOrderNumber(candidate).isPresent());
        return candidate;
    }
}
