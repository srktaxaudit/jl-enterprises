package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.dto.service.ServiceBookingDto;
import in.jlenterprises.ecommerce.entity.ServiceBooking;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.ServiceBookingRepository;
import in.jlenterprises.ecommerce.request.service.ServiceBookingRequest;
import in.jlenterprises.ecommerce.service.NotificationService;
import in.jlenterprises.ecommerce.service.ServiceBookingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class ServiceBookingServiceImpl implements ServiceBookingService {

    private static final Set<String> ALLOWED =
            Set.of("NEW", "CONTACTED", "SCHEDULED", "DONE", "CANCELLED");

    private final ServiceBookingRepository repository;
    private final NotificationService notificationService;

    public ServiceBookingServiceImpl(ServiceBookingRepository repository,
                                     NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public ServiceBookingDto create(ServiceBookingRequest request) {
        ServiceBooking b = new ServiceBooking();
        b.setCustomerName(request.name().trim());
        b.setPhone(request.phone().trim());
        b.setServiceType(request.serviceType());
        b.setMessage(request.message());
        b.setPreferredDate(request.preferredDate());
        b.setBookingStatus("NEW");
        ServiceBooking saved = repository.save(b);
        notificationService.notifyAdmins(NotificationType.SERVICE, "New service booking",
                "New service booking request received from " + saved.getCustomerName()
                        + (saved.getServiceType() != null ? " (" + saved.getServiceType() + ")" : "") + ".",
                "/admin-service.html", "Service Bookings", saved.getId(), "SERVICE_BOOKING");
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceBookingDto> list(String status, Pageable pageable) {
        Page<ServiceBooking> page = (status == null || status.isBlank())
                ? repository.findAll(pageable)
                : repository.findByBookingStatus(status.trim().toUpperCase(), pageable);
        return page.map(this::toDto);
    }

    @Override
    @Transactional
    public ServiceBookingDto updateStatus(UUID id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!ALLOWED.contains(normalized)) {
            throw new in.jlenterprises.ecommerce.exception.BusinessException("Invalid status: " + status);
        }
        ServiceBooking b = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ServiceBooking", id));
        b.setBookingStatus(normalized);
        return toDto(repository.save(b));
    }

    private ServiceBookingDto toDto(ServiceBooking b) {
        return new ServiceBookingDto(b.getId(), b.getCustomerName(), b.getPhone(), b.getServiceType(),
                b.getMessage(), b.getPreferredDate(), b.getBookingStatus(), b.getCreatedAt());
    }
}
