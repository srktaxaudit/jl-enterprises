package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.service.ServiceBookingDto;
import in.jlenterprises.ecommerce.request.service.ServiceBookingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ServiceBookingService {

    /** Public: record a new service request. */
    ServiceBookingDto create(ServiceBookingRequest request);

    /** Admin: list bookings (newest first), optionally filtered by status. */
    Page<ServiceBookingDto> list(String status, Pageable pageable);

    /** Admin: update the workflow status (NEW/CONTACTED/SCHEDULED/DONE/CANCELLED). */
    ServiceBookingDto updateStatus(UUID id, String status);
}
