package in.jlenterprises.ecommerce.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** A counter/phone (POS) sale entered by staff — a completed, cash-paid order. */
public record PosOrderRequest(
        @NotBlank String customerName,
        @NotBlank String customerPhone,
        String notes,
        BigDecimal discount,
        @NotEmpty @Valid List<Line> items
) {
    public record Line(@NotNull UUID productId, @Min(1) int quantity, BigDecimal unitPrice) {}
}
