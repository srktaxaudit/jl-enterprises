package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.emi.EmiRequestDto;
import in.jlenterprises.ecommerce.request.emi.EmiRequestCreate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EmiRequestService {

    /** Public: record a new EMI request (notifies admins). */
    EmiRequestDto create(EmiRequestCreate request);

    /** Admin: list requests (newest first), optionally filtered by status. */
    Page<EmiRequestDto> list(String status, Pageable pageable);

    /** Admin: update the workflow status (NEW/CONTACTED/CLOSED). */
    EmiRequestDto updateStatus(UUID id, String status);
}
