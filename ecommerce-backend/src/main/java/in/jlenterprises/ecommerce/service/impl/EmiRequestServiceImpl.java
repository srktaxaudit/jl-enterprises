package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.dto.emi.EmiRequestDto;
import in.jlenterprises.ecommerce.entity.EmiRequest;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.EmiRequestRepository;
import in.jlenterprises.ecommerce.request.emi.EmiRequestCreate;
import in.jlenterprises.ecommerce.service.EmiRequestService;
import in.jlenterprises.ecommerce.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class EmiRequestServiceImpl implements EmiRequestService {

    private static final Set<String> ALLOWED = Set.of("NEW", "CONTACTED", "CLOSED");

    private final EmiRequestRepository repository;
    private final NotificationService notificationService;

    public EmiRequestServiceImpl(EmiRequestRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public EmiRequestDto create(EmiRequestCreate request) {
        EmiRequest r = new EmiRequest();
        r.setCustomerName(request.name().trim());
        r.setPhone(request.phone().trim());
        r.setProductId(request.productId());
        r.setProductName(request.productName() == null || request.productName().isBlank() ? null : request.productName().trim());
        r.setMonths(request.months());
        r.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
        r.setEmiStatus("NEW");
        EmiRequest saved = repository.save(r);
        notificationService.notifyAdmins(NotificationType.EMI, "New EMI request",
                "New EMI request received from " + saved.getCustomerName()
                        + (saved.getProductName() != null ? " for " + saved.getProductName() : "") + ".",
                "/admin-emi-requests.html", "EMI Requests", saved.getId(), "EMI_REQUEST");
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmiRequestDto> list(String status, Pageable pageable) {
        Page<EmiRequest> page = (status == null || status.isBlank())
                ? repository.findAll(pageable)
                : repository.findByEmiStatus(status.trim().toUpperCase(), pageable);
        return page.map(this::toDto);
    }

    @Override
    @Transactional
    public EmiRequestDto updateStatus(UUID id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!ALLOWED.contains(normalized)) throw new BusinessException("Invalid status: " + status);
        EmiRequest r = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("EmiRequest", id));
        r.setEmiStatus(normalized);
        return toDto(repository.save(r));
    }

    private EmiRequestDto toDto(EmiRequest r) {
        return new EmiRequestDto(r.getId(), r.getCustomerName(), r.getPhone(), r.getProductId(),
                r.getProductName(), r.getMonths(), r.getNote(), r.getEmiStatus(), r.getCreatedAt());
    }
}
