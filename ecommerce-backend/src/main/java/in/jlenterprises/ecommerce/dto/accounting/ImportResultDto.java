package in.jlenterprises.ecommerce.dto.accounting;

import java.util.List;

/** Outcome of a bulk import. */
public record ImportResultDto(int created, int skipped, List<String> errors) {}
