package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.dto.stock.StockAlertDto;
import in.jlenterprises.ecommerce.entity.StockAlert;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.StockAlertRepository;
import in.jlenterprises.ecommerce.request.stock.StockAlertCreate;
import in.jlenterprises.ecommerce.service.NotificationService;
import in.jlenterprises.ecommerce.service.StockAlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class StockAlertServiceImpl implements StockAlertService {

    private static final Set<String> ALLOWED = Set.of("NEW", "NOTIFIED", "CLOSED");

    private final StockAlertRepository repository;
    private final NotificationService notificationService;

    public StockAlertServiceImpl(StockAlertRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public StockAlertDto create(StockAlertCreate request) {
        StockAlert a = new StockAlert();
        a.setCustomerName(request.name().trim());
        a.setPhone(request.phone().trim());
        a.setProductId(request.productId());
        a.setProductName(request.productName() == null || request.productName().isBlank() ? null : request.productName().trim());
        a.setAlertStatus("NEW");
        StockAlert saved = repository.save(a);
        notificationService.notifyAdmins(NotificationType.STOCK, "Back-in-stock request",
                saved.getCustomerName() + " wants to be notified when "
                        + (saved.getProductName() != null ? "\"" + saved.getProductName() + "\"" : "a product") + " is back in stock.",
                "/admin-stock-alerts.html", "Stock Alerts", saved.getId(), "STOCK_ALERT");
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockAlertDto> list(String status, Pageable pageable) {
        Page<StockAlert> page = (status == null || status.isBlank())
                ? repository.findAll(pageable)
                : repository.findByAlertStatus(status.trim().toUpperCase(), pageable);
        return page.map(this::toDto);
    }

    @Override
    @Transactional
    public StockAlertDto updateStatus(UUID id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!ALLOWED.contains(normalized)) throw new BusinessException("Invalid status: " + status);
        StockAlert a = repository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("StockAlert", id));
        a.setAlertStatus(normalized);
        return toDto(repository.save(a));
    }

    private StockAlertDto toDto(StockAlert a) {
        return new StockAlertDto(a.getId(), a.getCustomerName(), a.getPhone(), a.getProductId(),
                a.getProductName(), a.getAlertStatus(), a.getCreatedAt());
    }
}
