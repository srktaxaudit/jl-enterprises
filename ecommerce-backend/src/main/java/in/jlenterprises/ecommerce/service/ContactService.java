package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.contact.ContactEnquiryDto;
import in.jlenterprises.ecommerce.request.contact.ContactRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ContactService {

    /** Public: record a new contact enquiry (notifies admins). */
    ContactEnquiryDto create(ContactRequest request);

    /** Admin: list enquiries (newest first), optionally filtered by status. */
    Page<ContactEnquiryDto> list(String status, Pageable pageable);

    /** Admin: update the workflow status (NEW/READ/CLOSED). */
    ContactEnquiryDto updateStatus(UUID id, String status);
}
