package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.audit.Auditable;
import in.jlenterprises.ecommerce.dto.catalog.ProductDetailDto;
import in.jlenterprises.ecommerce.dto.catalog.ProductImageDto;
import in.jlenterprises.ecommerce.dto.catalog.ProductSearchCriteria;
import in.jlenterprises.ecommerce.dto.catalog.ProductSummaryDto;
import in.jlenterprises.ecommerce.entity.Brand;
import in.jlenterprises.ecommerce.entity.Category;
import in.jlenterprises.ecommerce.entity.Inventory;
import in.jlenterprises.ecommerce.entity.Product;
import in.jlenterprises.ecommerce.entity.ProductImage;
import in.jlenterprises.ecommerce.exception.DuplicateResourceException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.ProductMapper;
import in.jlenterprises.ecommerce.repository.BrandRepository;
import in.jlenterprises.ecommerce.repository.CategoryRepository;
import in.jlenterprises.ecommerce.repository.InventoryRepository;
import in.jlenterprises.ecommerce.repository.ProductRepository;
import in.jlenterprises.ecommerce.repository.ProductSpecifications;
import in.jlenterprises.ecommerce.request.catalog.ProductRequest;
import in.jlenterprises.ecommerce.service.ProductService;
import in.jlenterprises.ecommerce.storage.SupabaseStorageService;
import in.jlenterprises.ecommerce.util.SlugUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductMapper productMapper;
    private final SupabaseStorageService storageService;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository,
                              BrandRepository brandRepository, InventoryRepository inventoryRepository,
                              ProductMapper productMapper, SupabaseStorageService storageService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.inventoryRepository = inventoryRepository;
        this.productMapper = productMapper;
        this.storageService = storageService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryDto> search(ProductSearchCriteria c, Pageable pageable) {
        Specification<Product> spec = Specification.where(ProductSpecifications.search(c.search()))
                .and(ProductSpecifications.inCategorySlug(c.categorySlug()))
                .and(ProductSpecifications.inBrandSlug(c.brandSlug()))
                .and(ProductSpecifications.priceGoe(c.minPrice()))
                .and(ProductSpecifications.priceLoe(c.maxPrice()))
                .and(ProductSpecifications.featured(c.featured()))
                .and(ProductSpecifications.minRating(c.minRating()))
                .and(ProductSpecifications.inStock(c.inStock()))
                .and(ProductSpecifications.emiAvailable(c.emiAvailable()));
        return productRepository.findAll(spec, pageable).map(productMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryDto> searchPublic(ProductSearchCriteria c, Pageable pageable) {
        // Same filters as admin search, but only ACTIVE + in-stock products.
        Specification<Product> spec = Specification.where(ProductSpecifications.visible())
                .and(ProductSpecifications.search(c.search()))
                .and(ProductSpecifications.inCategorySlug(c.categorySlug()))
                .and(ProductSpecifications.inBrandSlug(c.brandSlug()))
                .and(ProductSpecifications.priceGoe(c.minPrice()))
                .and(ProductSpecifications.priceLoe(c.maxPrice()))
                .and(ProductSpecifications.featured(c.featured()))
                .and(ProductSpecifications.minRating(c.minRating()));
        return productRepository.findAll(spec, pageable).map(productMapper::toSummary);
    }

    @Override
    @Transactional
    public ProductDetailDto getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", slug));
        product.setViewCount(product.getViewCount() + 1);
        // Expose available stock so the storefront can disable Add to Cart when out of stock.
        int available = inventoryRepository.findByProductId(product.getId())
                .map(Inventory::getAvailable).orElse(0);
        product.setAvailableStock(available);
        return productMapper.toDetail(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryDto> featured(Pageable pageable) {
        // Featured carousel is customer-facing → only ACTIVE, in-stock, featured products.
        Specification<Product> spec = ProductSpecifications.visible()
                .and(ProductSpecifications.featured(true));
        return productRepository.findAll(spec, pageable).map(productMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryDto> related(String slug, int limit) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", slug));
        if (product.getCategory() == null) return List.of();
        UUID categoryId = product.getCategory().getId();
        // Related products must also be visible (active + in stock).
        Specification<Product> spec = ProductSpecifications.visible()
                .and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        return productRepository.findAll(spec, PageRequest.of(0, limit + 1)).getContent().stream()
                .filter(p -> !p.getId().equals(product.getId()))
                .limit(limit)
                .map(productMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    @Auditable(action = "CREATE_PRODUCT", entity = "product")
    public ProductDetailDto create(ProductRequest request) {
        String slug = resolveSlug(request);
        if (productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Product slug already exists: " + slug);
        }
        if (productRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("Product SKU already exists: " + request.sku());
        }
        Product product = new Product();
        product.setSlug(slug);
        apply(product, request);
        product = productRepository.save(product);

        // Every product gets a stock record so inventory management has a home.
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventoryRepository.save(inventory);

        return productMapper.toDetail(product);
    }

    @Override
    @Transactional
    @Auditable(action = "UPDATE_PRODUCT", entity = "product")
    public ProductDetailDto update(UUID id, ProductRequest request) {
        Product product = getEntity(id);
        String slug = resolveSlug(request);
        if (!slug.equals(product.getSlug()) && productRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Product slug already exists: " + slug);
        }
        product.setSlug(slug);
        apply(product, request);
        return productMapper.toDetail(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductDetailDto setFeatured(UUID id, boolean featured) {
        Product product = getEntity(id);
        product.setFeatured(featured);
        return productMapper.toDetail(productRepository.save(product));
    }

    @Override
    @Transactional
    @Auditable(action = "DELETE_PRODUCT", entity = "product")
    public void delete(UUID id) {
        Product product = getEntity(id);
        product.setDeleted(true);
        productRepository.save(product);
    }

    // ── Product images ──
    @Override
    @Transactional(readOnly = true)
    public List<ProductImageDto> listImages(UUID productId) {
        return imageDtos(getEntity(productId));
    }

    @Override
    @Transactional
    public List<ProductImageDto> addImage(UUID productId, String url, String altText) {
        Product product = getEntity(productId);
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setUrl(url);
        image.setAltText(altText);
        image.setSortOrder(product.getImages().stream().mapToInt(ProductImage::getSortOrder).max().orElse(-1) + 1);
        image.setPrimary(product.getImages().isEmpty());   // first image is primary by default
        product.getImages().add(image);
        productRepository.save(product);                   // cascade persists the new image
        return imageDtos(product);
    }

    @Override
    @Transactional
    public List<ProductImageDto> deleteImage(UUID productId, UUID imageId) {
        Product product = getEntity(productId);
        ProductImage target = product.getImages().stream()
                .filter(i -> i.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("ProductImage", imageId));
        boolean wasPrimary = target.isPrimary();
        String url = target.getUrl();
        product.getImages().remove(target);               // orphanRemoval deletes the row
        if (wasPrimary && !product.getImages().isEmpty()) {
            product.getImages().get(0).setPrimary(true);   // promote a new primary
        }
        productRepository.save(product);
        storageService.deleteByPublicUrl(url);             // best-effort; never throws
        return imageDtos(product);
    }

    @Override
    @Transactional
    public List<ProductImageDto> setPrimaryImage(UUID productId, UUID imageId) {
        Product product = getEntity(productId);
        boolean found = false;
        for (ProductImage img : product.getImages()) {
            boolean isTarget = img.getId().equals(imageId);
            img.setPrimary(isTarget);
            found = found || isTarget;
        }
        if (!found) throw ResourceNotFoundException.of("ProductImage", imageId);
        productRepository.save(product);
        return imageDtos(product);
    }

    private List<ProductImageDto> imageDtos(Product product) {
        return product.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::isPrimary).reversed()
                        .thenComparingInt(ProductImage::getSortOrder))
                .map(productMapper::toImageDto)
                .toList();
    }

    // ── helpers ──
    private void apply(Product p, ProductRequest r) {
        p.setName(r.name());
        p.setSku(r.sku());
        p.setShortDescription(r.shortDescription());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setComparePrice(r.comparePrice());
        p.setDiscountPercent(r.discountPercent());
        if (r.currency() != null && !r.currency().isBlank()) p.setCurrency(r.currency());
        if (r.featured() != null) p.setFeatured(r.featured());
        p.setMetaTitle(r.metaTitle());
        p.setMetaDescription(r.metaDescription());
        p.setCategory(r.categoryId() == null ? null : findCategory(r.categoryId()));
        p.setBrand(r.brandId() == null ? null : findBrand(r.brandId()));
        // EMI — stored exactly as the admin enters it; the storefront never auto-calculates.
        p.setEmiAvailable(r.emiAvailable() != null && r.emiAvailable());
        p.setEmiMonths(r.emiMonths());
        p.setEmiAmount(r.emiAmount());
        p.setEmiDownPayment(r.emiDownPayment());
        p.setEmiProcessingFee(r.emiProcessingFee());
        p.setEmiNote(r.emiNote());
    }

    private String resolveSlug(ProductRequest request) {
        String base = (request.slug() != null && !request.slug().isBlank()) ? request.slug() : request.name();
        return SlugUtil.slugify(base);
    }

    private Category findCategory(UUID id) {
        return categoryRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    private Brand findBrand(UUID id) {
        return brandRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Brand", id));
    }

    private Product getEntity(UUID id) {
        return productRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }
}
