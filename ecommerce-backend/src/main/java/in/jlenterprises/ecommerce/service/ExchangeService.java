package in.jlenterprises.ecommerce.service;

import in.jlenterprises.ecommerce.constant.ExchangeStatus;
import in.jlenterprises.ecommerce.dto.exchange.ExchangeRequestDto;
import in.jlenterprises.ecommerce.request.exchange.ExchangeCreateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ExchangeService {

    // ── Customer ──
    ExchangeRequestDto create(UUID userId, ExchangeCreateRequest request);

    List<ExchangeRequestDto> listMine(UUID userId);

    /** Approved requests (with a final value) not yet applied — offered at checkout. */
    List<ExchangeRequestDto> checkoutOptions(UUID userId);

    /** Upload one image and return its public URL (call before create). */
    String uploadImage(UUID userId, MultipartFile file);

    // ── Admin ──
    Page<ExchangeRequestDto> list(ExchangeStatus status, Pageable pageable);

    ExchangeRequestDto get(UUID id);

    ExchangeRequestDto updateStatus(UUID id, ExchangeStatus status, String internalNotes);

    ExchangeRequestDto setFinalValue(UUID id, BigDecimal finalValue);

    // ── Checkout integration (called by OrderService) ──
    /** Validate the exchange belongs to the user, is approved and unused; return its value. */
    BigDecimal valueForCheckout(UUID userId, UUID exchangeId);

    /** Mark the exchange consumed by an order (COMPLETED + linked). */
    void applyToOrder(UUID exchangeId, UUID orderId);
}
