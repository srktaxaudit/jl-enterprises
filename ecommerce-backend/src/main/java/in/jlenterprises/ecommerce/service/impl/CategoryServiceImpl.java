package in.jlenterprises.ecommerce.service.impl;

import in.jlenterprises.ecommerce.dto.catalog.CategoryDto;
import in.jlenterprises.ecommerce.entity.Category;
import in.jlenterprises.ecommerce.exception.DuplicateResourceException;
import in.jlenterprises.ecommerce.exception.ResourceNotFoundException;
import in.jlenterprises.ecommerce.mapper.CategoryMapper;
import in.jlenterprises.ecommerce.repository.CategoryRepository;
import in.jlenterprises.ecommerce.request.catalog.CategoryRequest;
import in.jlenterprises.ecommerce.service.CategoryService;
import in.jlenterprises.ecommerce.util.SlugUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryServiceImpl(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        return categoryMapper.toDtoList(categoryRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getBySlug(String slug) {
        return categoryMapper.toDto(categoryRepository.findBySlug(slug)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", slug)));
    }

    @Override
    @Transactional
    public CategoryDto create(CategoryRequest request) {
        String slug = resolveSlug(request);
        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Category slug already exists: " + slug);
        }
        Category category = new Category();
        apply(category, request, slug);
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryDto update(UUID id, CategoryRequest request) {
        Category category = getEntity(id);
        String slug = resolveSlug(request);
        if (!slug.equals(category.getSlug()) && categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Category slug already exists: " + slug);
        }
        apply(category, request, slug);
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Category category = getEntity(id);
        category.setDeleted(true);
        categoryRepository.save(category);
    }

    // ── helpers ──
    private void apply(Category category, CategoryRequest request, String slug) {
        category.setName(request.name());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        if (request.parentId() != null) {
            category.setParent(getEntity(request.parentId()));
        } else {
            category.setParent(null);
        }
    }

    private String resolveSlug(CategoryRequest request) {
        String base = (request.slug() != null && !request.slug().isBlank()) ? request.slug() : request.name();
        return SlugUtil.slugify(base);
    }

    private Category getEntity(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }
}
