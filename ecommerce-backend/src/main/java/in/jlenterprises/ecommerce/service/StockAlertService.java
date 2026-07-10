package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.dto.stock.StockAlertDto;
import in.jlenterprises.ecommerce.request.stock.StockAlertCreate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockAlertService {

    /** Public: record a back-in-stock request (notifies admins). */
    StockAlertDto create(StockAlertCreate request);

    /** Admin: list alerts (newest first), optionally filtered by status. */
    Page<StockAlertDto> list(String status, Pageable pageable);

    /** Admin: update the workflow status (NEW/NOTIFIED/CLOSED). */
    StockAlertDto updateStatus(UUID id, String status);
}
