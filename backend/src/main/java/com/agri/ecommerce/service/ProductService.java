package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.common.utils.SlugUtils;
import com.agri.ecommerce.dto.request.ProductImageRequest;
import com.agri.ecommerce.dto.request.ProductRequest;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.dto.response.ProductImageResponse;
import com.agri.ecommerce.dto.response.ProductResponse;
import com.agri.ecommerce.entity.Category;
import com.agri.ecommerce.entity.Product;
import com.agri.ecommerce.entity.ProductImage;
import com.agri.ecommerce.entity.ProductStatus;
import com.agri.ecommerce.mapper.CatalogMapper;
import com.agri.ecommerce.repository.ProductImageRepository;
import com.agri.ecommerce.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryService categoryService;

    public ProductService(
        ProductRepository productRepository,
        ProductImageRepository productImageRepository,
        CategoryService categoryService
    ) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryService = categoryService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findPublicProducts(
        String search,
        String categorySlug,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String status,
        Pageable pageable
    ) {
        validatePriceRange(minPrice, maxPrice);
        ProductStatus parsedStatus = parseStatus(status);
        if (parsedStatus == ProductStatus.HIDDEN) {
            throw new ApplicationException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PUBLIC_STATUS",
                "Trang thai hidden khong kha dung tren catalog cong khai"
            );
        }

        return PageResponse.from(
            productRepository.findAll(filter(
                search,
                categorySlug,
                null,
                minPrice,
                maxPrice,
                parsedStatus,
                false
            ), pageable),
            CatalogMapper::toProductResponse
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse findPublicProduct(String slug) {
        Product product = productRepository.findBySlugIgnoreCase(slug)
            .filter(item -> item.getStatus() != ProductStatus.HIDDEN)
            .orElseThrow(() -> notFound("PRODUCT_NOT_FOUND", "Khong tim thay san pham"));
        return CatalogMapper.toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAdminProducts(
        String search,
        Long categoryId,
        String status,
        Pageable pageable
    ) {
        return PageResponse.from(
            productRepository.findAll(filter(
                search,
                null,
                categoryId,
                null,
                null,
                parseStatus(status),
                true
            ), pageable),
            CatalogMapper::toProductResponse
        );
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (request.stock() != 0) {
            throw new ApplicationException(HttpStatus.CONFLICT, "INVENTORY_IMPORT_REQUIRED", "Hãy nhập kho theo lô để tăng tồn kho");
        }
        Product product = new Product();
        apply(product, request);
        product.setSlug(uniqueSlug(request.slug(), request.name(), null));
        return CatalogMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long productId, ProductRequest request) {
        Product product = lockEntity(productId);
        if (request.stock() != product.getStock()) {
            throw new ApplicationException(HttpStatus.CONFLICT, "INVENTORY_ADJUSTMENT_REQUIRED", "Hãy dùng nhập kho hoặc điều chỉnh kho để thay đổi tồn kho");
        }
        apply(product, request);
        product.setSlug(uniqueSlug(request.slug(), request.name(), productId));
        return CatalogMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateStatus(Long productId, String status) {
        Product product = lockEntity(productId);
        product.setStatus(requireStatus(status));
        return CatalogMapper.toProductResponse(productRepository.save(product));
    }

    @Transactional
    public ProductImageResponse addImage(Long productId, ProductImageRequest request) {
        Product product = findEntity(productId);
        ProductImage productImage = new ProductImage();
        productImage.setProduct(product);
        productImage.setImage(request.image().trim());
        ProductImage saved = productImageRepository.save(productImage);
        return new ProductImageResponse(saved.getId(), saved.getImage());
    }

    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
            .orElseThrow(() -> notFound("PRODUCT_IMAGE_NOT_FOUND", "Khong tim thay anh san pham"));
        if (!image.getProduct().getId().equals(productId)) {
            throw notFound("PRODUCT_IMAGE_NOT_FOUND", "Khong tim thay anh san pham");
        }
        productImageRepository.delete(image);
    }

    private Product findEntity(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> notFound("PRODUCT_NOT_FOUND", "Khong tim thay san pham"));
    }

    private Product lockEntity(Long productId) {
        return productRepository.findAllByIdForUpdate(List.of(productId)).stream().findFirst()
            .orElseThrow(() -> notFound("PRODUCT_NOT_FOUND", "Khong tim thay san pham"));
    }

    private void apply(Product product, ProductRequest request) {
        Category category = categoryService.findEntity(request.categoryId());
        product.setName(request.name().trim());
        product.setNameEn(trimToNull(request.nameEn()));
        product.setCategory(category);
        product.setDescription(trimToNull(request.description()));
        product.setDescriptionEn(trimToNull(request.descriptionEn()));
        product.setPrice(request.price());
        // Product.stock is a cache maintained by InventoryService.
        ProductStatus requestedStatus = StringUtils.hasText(request.status())
            ? requireStatus(request.status())
            : product.getStatus();
        if (requestedStatus == null) {
            requestedStatus = request.stock() == 0
                ? ProductStatus.OUT_OF_STOCK
                : ProductStatus.IN_STOCK;
        }
        if (request.stock() == 0 && requestedStatus == ProductStatus.IN_STOCK) {
            requestedStatus = ProductStatus.OUT_OF_STOCK;
        }
        product.setStatus(requestedStatus);
        product.setUnit(trimToNull(request.unit()));
        product.setUnitEn(trimToNull(request.unitEn()));
    }

    private Specification<Product> filter(
        String search,
        String categorySlug,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ProductStatus status,
        boolean includeHidden
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)
                ));
            }
            if (StringUtils.hasText(categorySlug)) {
                predicates.add(criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("category").get("slug")),
                    categorySlug.trim().toLowerCase()
                ));
            }
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            } else if (!includeHidden) {
                predicates.add(criteriaBuilder.notEqual(root.get("status"), ProductStatus.HIDDEN));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validatePriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.signum() < 0
            || maxPrice != null && maxPrice.signum() < 0) {
            throw new ApplicationException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PRICE_RANGE",
                "Khoang gia khong duoc am"
            );
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ApplicationException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PRICE_RANGE",
                "Gia toi thieu khong duoc lon hon gia toi da"
            );
        }
    }

    private ProductStatus parseStatus(String status) {
        return StringUtils.hasText(status) ? requireStatus(status) : null;
    }

    private ProductStatus requireStatus(String status) {
        try {
            return ProductStatus.from(status.trim());
        } catch (IllegalArgumentException exception) {
            throw new ApplicationException(
                HttpStatus.BAD_REQUEST,
                "INVALID_PRODUCT_STATUS",
                exception.getMessage()
            );
        }
    }

    private String uniqueSlug(String requestedSlug, String name, Long currentId) {
        String base = SlugUtils.slugify(StringUtils.hasText(requestedSlug) ? requestedSlug : name);
        String candidate = base;
        int suffix = 2;
        while (currentId == null
            ? productRepository.existsBySlugIgnoreCase(candidate)
            : productRepository.existsBySlugIgnoreCaseAndIdNot(candidate, currentId)) {
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
}
