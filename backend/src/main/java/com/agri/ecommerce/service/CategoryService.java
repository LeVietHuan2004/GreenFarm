package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.common.utils.SlugUtils;
import com.agri.ecommerce.dto.request.CategoryRequest;
import com.agri.ecommerce.dto.response.CategoryResponse;
import com.agri.ecommerce.entity.Category;
import com.agri.ecommerce.entity.ProductStatus;
import com.agri.ecommerce.mapper.CatalogMapper;
import com.agri.ecommerce.repository.CategoryRepository;
import com.agri.ecommerce.repository.ProductRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(
        CategoryRepository categoryRepository,
        ProductRepository productRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findPublicCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
            .map(category -> CatalogMapper.toCategoryResponse(
                category,
                productRepository.countByCategoryIdAndStatusNot(
                    category.getId(),
                    ProductStatus.HIDDEN
                )
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAdminCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
            .map(category -> CatalogMapper.toCategoryResponse(
                category,
                productRepository.countByCategoryId(category.getId())
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findBySlug(String slug) {
        Category category = categoryRepository.findBySlugIgnoreCase(slug)
            .orElseThrow(() -> notFound("CATEGORY_NOT_FOUND", "Khong tim thay danh muc"));
        return CatalogMapper.toCategoryResponse(
            category,
            productRepository.countByCategoryIdAndStatusNot(category.getId(), ProductStatus.HIDDEN)
        );
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw conflict("CATEGORY_NAME_EXISTS", "Ten danh muc da ton tai");
        }

        Category category = new Category();
        apply(category, request);
        category.setSlug(uniqueSlug(request.slug(), name, null));
        Category saved = categoryRepository.save(category);
        return CatalogMapper.toCategoryResponse(saved, 0);
    }

    @Transactional
    public CategoryResponse update(Long categoryId, CategoryRequest request) {
        Category category = findEntity(categoryId);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, categoryId)) {
            throw conflict("CATEGORY_NAME_EXISTS", "Ten danh muc da ton tai");
        }

        apply(category, request);
        category.setSlug(uniqueSlug(request.slug(), name, categoryId));
        Category saved = categoryRepository.save(category);
        return CatalogMapper.toCategoryResponse(
            saved,
            productRepository.countByCategoryId(categoryId)
        );
    }

    @Transactional
    public void delete(Long categoryId) {
        Category category = findEntity(categoryId);
        if (productRepository.existsByCategoryId(categoryId)) {
            throw conflict(
                "CATEGORY_IN_USE",
                "Khong the xoa danh muc dang co san pham"
            );
        }
        categoryRepository.delete(category);
    }

    Category findEntity(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> notFound("CATEGORY_NOT_FOUND", "Khong tim thay danh muc"));
    }

    private void apply(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setNameEn(trimToNull(request.nameEn()));
        category.setDescription(trimToNull(request.description()));
        category.setDescriptionEn(trimToNull(request.descriptionEn()));
        category.setImage(trimToNull(request.image()));
    }

    private String uniqueSlug(String requestedSlug, String name, Long currentId) {
        String base = SlugUtils.slugify(StringUtils.hasText(requestedSlug) ? requestedSlug : name);
        String candidate = base;
        int suffix = 2;
        while (currentId == null
            ? categoryRepository.existsBySlugIgnoreCase(candidate)
            : categoryRepository.existsBySlugIgnoreCaseAndIdNot(candidate, currentId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String trimToNull(String value) {
        return !StringUtils.hasText(value) ? null : value.trim();
    }

    private ApplicationException notFound(String code, String message) {
        return new ApplicationException(HttpStatus.NOT_FOUND, code, message);
    }

    private ApplicationException conflict(String code, String message) {
        return new ApplicationException(HttpStatus.CONFLICT, code, message);
    }
}
