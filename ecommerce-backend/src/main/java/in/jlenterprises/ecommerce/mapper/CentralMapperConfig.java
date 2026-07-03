package in.jlenterprises.ecommerce.mapper;

import org.mapstruct.MapperConfig;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * Shared MapStruct configuration referenced by every mapper
 * ({@code @Mapper(config = CentralMapperConfig.class)}).
 *
 * - Spring component model → mappers are injectable beans.
 * - Unmapped targets are ignored (DTOs intentionally expose a subset).
 * - Null source properties are skipped on updates, enabling PATCH semantics.
 */
@MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CentralMapperConfig {
}
