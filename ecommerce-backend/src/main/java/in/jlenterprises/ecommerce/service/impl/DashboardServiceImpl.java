package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.dto.admin.DashboardStatsDto;
import in.jlenterprises.ecommerce.repository.InventoryRepository;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Duration WINDOW = Duration.ofDays(30);

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;

    public DashboardServiceImpl(UserRepository userRepository, ProductRepository productRepository,
                                OrderRepository orderRepository, InventoryRepository inventoryRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getStats() {
        Instant since = Instant.now().minus(WINDOW);
        BigDecimal revenue = orderRepository.revenueSince(since);
        return new DashboardStatsDto(
                userRepository.count(),
                productRepository.count(),
                orderRepository.count(),
                orderRepository.countByOrderStatus(OrderStatus.PENDING),
                inventoryRepository.countLowStock(),
                orderRepository.countPlacedSince(since),
                revenue == null ? BigDecimal.ZERO : revenue,
                inventoryRepository.countActive(),
                inventoryRepository.countInStock(),
                inventoryRepository.countOutOfStock(),
                orderRepository.countByOrderStatus(OrderStatus.PROCESSING),
                orderRepository.countByOrderStatus(OrderStatus.DELIVERED),
                orderRepository.countByOrderStatus(OrderStatus.CANCELLED),
                orderRepository.countByOrderStatus(OrderStatus.RETURNED)
        );
    }
}
