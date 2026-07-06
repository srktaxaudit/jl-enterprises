package in.jlenterprises.ecommerce.request.accounting;

import in.jlenterprises.ecommerce.constant.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DocumentRequest(
        @NotNull DocumentType documentType,
        @NotNull LocalDate documentDate,
        UUID partyAccountId,
        @Size(max = 160) String partyName,
        @Size(max = 20) String partyGstin,
        @Size(max = 500) String billingAddress,
        @Size(max = 500) String shippingAddress,
        @Size(max = 80) String placeOfSupply,
        Boolean interState,
        @Size(max = 500) String narration,
        @Size(max = 120) String reference,
        UUID referenceDocumentId,
        @Size(max = 20) String tdsSection,
        @PositiveOrZero BigDecimal tdsRate,
        @NotEmpty @Valid List<Line> lines
) {
    public record Line(
            @Size(max = 300) String description,
            @Size(max = 20) String hsnCode,
            UUID productId,
            @PositiveOrZero BigDecimal quantity,
            @PositiveOrZero BigDecimal rate,
            @PositiveOrZero BigDecimal discountPercent,
            @PositiveOrZero BigDecimal gstRate
    ) {}
}
