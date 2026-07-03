package in.jlenterprises.ecommerce.mapper;

import in.jlenterprises.ecommerce.dto.catalog.ProductDetailDto;
import in.jlenterprises.ecommerce.dto.catalog.ProductImageDto;
import in.jlenterprises.ecommerce.dto.catalog.ProductSummaryDto;
import in.jlenterprises.ecommerce.dto.catalog.ProductVariantDto;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.entity.ProductImage;
import in.jlenterprises.ecommerce.entity.ProductVariant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Product ↔ DTO mapping. Nested brand/category fields are flattened, and the
 * primary image URL is derived via a helper (primary flag first, else the
 * first image).
 */
@Mapper(config = CentralMapperConfig.class)
public interface ProductMapper {

    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "categorySlug", source = "category.slug")
    @Mapping(target = "primaryImageUrl", expression = "java(primaryImage(product))")
    ProductSummaryDto toSummary(Product product);

    List<ProductSummaryDto> toSummaryList(List<Product> products);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categorySlug", source = "category.slug")
    @Mapping(target = "brandId", source = "brand.id")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "primaryImageUrl", expression = "java(primaryImage(product))")
    ProductDetailDto toDetail(Product product);

    ProductImageDto toImageDto(ProductImage image);

    ProductVariantDto toVariantDto(ProductVariant variant);

    /** Prefer the image flagged primary; otherwise fall back to the first image. */
    default String primaryImage(Product product) {
        if (product == null || product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .filter(ProductImage::isPrimary)
                .map(ProductImage::getUrl)
                .findFirst()
                .orElseGet(() -> product.getImages().get(0).getUrl());
    }
}
