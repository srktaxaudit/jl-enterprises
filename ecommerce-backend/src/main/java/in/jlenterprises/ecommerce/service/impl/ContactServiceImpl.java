package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.constant.NotificationType;
import in.jlenterprises.ecommerce.dto.contact.ContactEnquiryDto;
import in.jlenterprises.ecommerce.entity.ContactEnquiry;
import in.jlenterprises.ecommerce.exception.BusinessException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.repository.ContactEnquiryRepository;
import in.jlenterprises.ecommerce.request.contact.ContactRequest;
import in.jlenterprises.ecommerce.service.ContactService;
import in.jlenterprises.ecommerce.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class ContactServiceImpl implements ContactService {

    private static final Set<String> ALLOWED = Set.of("NEW", "READ", "CLOSED");

    private final ContactEnquiryRepository repository;
    private final NotificationService notificationService;

    public ContactServiceImpl(ContactEnquiryRepository repository, NotificationService notificationService) {
        this.repository = repository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public ContactEnquiryDto create(ContactRequest request) {
        ContactEnquiry e = new ContactEnquiry();
        e.setName(request.name().trim());
        e.setEmail(request.email() == null || request.email().isBlank() ? null : request.email().trim());
        e.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        e.setSubject(request.subject() == null || request.subject().isBlank() ? null : request.subject().trim());
        e.setMessage(request.message().trim());
        e.setEnquiryStatus("NEW");
        ContactEnquiry saved = repository.save(e);
        notificationService.notifyAdmins(NotificationType.CONTACT, "New contact enquiry",
                "User submitted a contact form: " + saved.getName()
                        + (saved.getSubject() != null ? " — " + saved.getSubject() : "") + ".",
                "/admin-enquiries.html", "Enquiries", saved.getId(), "CONTACT_ENQUIRY");
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContactEnquiryDto> list(String status, Pageable pageable) {
        Page<ContactEnquiry> page = (status == null || status.isBlank())
                ? repository.findAll(pageable)
                : repository.findByEnquiryStatus(status.trim().toUpperCase(), pageable);
        return page.map(this::toDto);
    }

    @Override
    @Transactional
    public ContactEnquiryDto updateStatus(UUID id, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!ALLOWED.contains(normalized)) throw new BusinessException("Invalid status: " + status);
        ContactEnquiry e = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ContactEnquiry", id));
        e.setEnquiryStatus(normalized);
        return toDto(repository.save(e));
    }

    private ContactEnquiryDto toDto(ContactEnquiry e) {
        return new ContactEnquiryDto(e.getId(), e.getName(), e.getEmail(), e.getPhone(),
                e.getSubject(), e.getMessage(), e.getEnquiryStatus(), e.getCreatedAt());
    }
}
