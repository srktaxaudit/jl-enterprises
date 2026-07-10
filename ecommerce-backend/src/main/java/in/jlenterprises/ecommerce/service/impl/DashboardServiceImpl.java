package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.OrderStatus;
import in.jlenterprises.ecommerce.dto.admin.DashboardStatsDto;
import in.jlenterprises.ecommerce.dto.admin.SalesAnalyticsDto;
import in.jlenterprises.ecommerce.dto.admin.SalesAnalyticsDto.CategorySlice;
import in.jlenterprises.ecommerce.dto.admin.SalesAnalyticsDto.TopProduct;
import in.jlenterprises.ecommerce.dto.admin.SalesAnalyticsDto.TrendPoint;
import in.jlenterprises.ecommerce.repository.InventoryRepository;
import in.jlenterprises.ecommerce.repository.OrderRepository;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import in.jlenterprises.ecommerce.repository.UserRepository;
import in.jlenterprises.ecommerce.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @Override
    @Transactional(readOnly = true)
    public SalesAnalyticsDto getSalesAnalytics(int days) {
        int d = Math.min(Math.max(days, 1), 365);
        LocalDate today = LocalDate.now(ZONE);
        LocalDate start = today.minusDays(d - 1L);
        Instant from = start.atStartOfDay(ZONE).toInstant();

        // Revenue trend — bucket each non-cancelled order into its local day.
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        long count = 0;
        for (Object[] row : orderRepository.revenueRowsSince(from)) {
            Instant placed = (Instant) row[0];
            BigDecimal amount = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
            byDay.merge(placed.atZone(ZONE).toLocalDate(), amount, BigDecimal::add);
            total = total.add(amount);
            count++;
        }
        List<TrendPoint> trend = new ArrayList<>(d);
        for (int i = 0; i < d; i++) {
            LocalDate day = start.plusDays(i);
            trend.add(new TrendPoint(day.toString(), byDay.getOrDefault(day, BigDecimal.ZERO)));
        }
        BigDecimal aov = count > 0 ? total.divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Best-sellers (top 10 by units) and category revenue mix.
        List<TopProduct> top = new ArrayList<>();
        for (Object[] row : orderRepository.topProductsSince(from, PageRequest.of(0, 10))) {
            top.add(new TopProduct((String) row[0], ((Number) row[1]).longValue(),
                    row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2]));
        }
        List<CategorySlice> categories = new ArrayList<>();
        for (Object[] row : orderRepository.categoryRevenueSince(from)) {
            categories.add(new CategorySlice((String) row[0], row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1]));
        }
        return new SalesAnalyticsDto(d, total, count, aov, trend, top, categories);
    }
}
