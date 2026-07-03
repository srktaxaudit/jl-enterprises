package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.dto.catalog.BrandDto;
import in.jlenterprises.ecommerce.entity.Brand;
import in.jlenterprises.ecommerce.exception.DuplicateResourceException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.BrandMapper;
import in.jlenterprises.ecommerce.repository.BrandRepository;
import in.jlenterprises.ecommerce.request.catalog.BrandRequest;
import in.jlenterprises.ecommerce.service.BrandService;
import in.jlenterprises.ecommerce.util.SlugUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    public BrandServiceImpl(BrandRepository brandRepository, BrandMapper brandMapper) {
        this.brandRepository = brandRepository;
        this.brandMapper = brandMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandDto> list() {
        return brandMapper.toDtoList(brandRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public BrandDto getBySlug(String slug) {
        return brandMapper.toDto(brandRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.of("Brand", slug)));
    }

    @Override
    @Transactional
    public BrandDto create(BrandRequest request) {
        String slug = resolveSlug(request);
        if (brandRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Brand slug already exists: " + slug);
        }
        Brand brand = new Brand();
        apply(brand, request, slug);
        return brandMapper.toDto(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public BrandDto update(UUID id, BrandRequest request) {
        Brand brand = getEntity(id);
        String slug = resolveSlug(request);
        if (!slug.equals(brand.getSlug()) && brandRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Brand slug already exists: " + slug);
        }
        apply(brand, request, slug);
        return brandMapper.toDto(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Brand brand = getEntity(id);
        brand.setDeleted(true);
        brandRepository.save(brand);
    }

    private void apply(Brand brand, BrandRequest request, String slug) {
        brand.setName(request.name());
        brand.setSlug(slug);
        brand.setLogoUrl(request.logoUrl());
        brand.setDescription(request.description());
    }

    private String resolveSlug(BrandRequest request) {
        String base = (request.slug() != null && !request.slug().isBlank()) ? request.slug() : request.name();
        return SlugUtil.slugify(base);
    }

    private Brand getEntity(UUID id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Brand", id));
    }
}
