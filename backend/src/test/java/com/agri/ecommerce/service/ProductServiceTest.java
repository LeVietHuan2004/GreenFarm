package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.ProductRequest;
import com.agri.ecommerce.entity.Category;
import com.agri.ecommerce.entity.Product;
import com.agri.ecommerce.entity.ProductStatus;
import com.agri.ecommerce.repository.ProductImageRepository;
import com.agri.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private CategoryService categoryService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(
            productRepository,
            productImageRepository,
            categoryService
        );
    }

    @Test
    void marksNewProductOutOfStockWhenStockIsZero() {
        Category category = new Category();
        category.setName("Rau cu");
        category.setSlug("rau-cu");
        when(categoryService.findEntity(1L)).thenReturn(category);
        when(productRepository.save(any(Product.class)))
            .thenAnswer(call -> call.getArgument(0));

        ProductRequest request = new ProductRequest(
            "Ca chua",
            null,
            null,
            1L,
            "Ca chua tuoi",
            null,
            new BigDecimal("25000"),
            0,
            null,
            "kg",
            null
        );

        var response = productService.create(request);

        assertThat(response.slug()).isEqualTo("ca-chua");
        assertThat(response.status()).isEqualTo("out_of_stock");
    }

    @Test
    void stockCannotBeSetThroughProductEditor() {
        ProductRequest request = new ProductRequest("Ca chua", null, null, 1L, null, null,
            new BigDecimal("25000"), 12, null, "kg", null);
        assertThatThrownBy(() -> productService.create(request))
            .extracting("code").isEqualTo("INVENTORY_IMPORT_REQUIRED");
        verify(productRepository, never()).save(any());
    }

    @Test
    void stockCannotBeChangedThroughProductUpdate() {
        Product product = new Product(); product.setStock(5);
        when(productRepository.findAllByIdForUpdate(List.of(1L))).thenReturn(List.of(product));
        ProductRequest request = new ProductRequest("Ca chua", null, null, 1L, null, null,
            new BigDecimal("25000"), 6, null, "kg", null);
        assertThatThrownBy(() -> productService.update(1L, request))
            .extracting("code").isEqualTo("INVENTORY_ADJUSTMENT_REQUIRED");
        verify(productRepository, never()).save(any());
    }

    @Test
    void hidesHiddenProductFromPublicDetail() {
        Product product = new Product();
        product.setStatus(ProductStatus.HIDDEN);
        when(productRepository.findBySlugIgnoreCase("san-pham-an"))
            .thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.findPublicProduct("san-pham-an"))
            .isInstanceOf(ApplicationException.class)
            .extracting("code")
            .isEqualTo("PRODUCT_NOT_FOUND");
    }

    @Test
    void rejectsInvalidPriceRangeBeforeQueryingDatabase() {
        assertThatThrownBy(() -> productService.findPublicProducts(
            null,
            null,
            new BigDecimal("50000"),
            new BigDecimal("10000"),
            null,
            PageRequest.of(0, 12)
        ))
            .isInstanceOf(ApplicationException.class)
            .extracting("code")
            .isEqualTo("INVALID_PRICE_RANGE");

        verify(productRepository, never()).findAll(
            org.mockito.ArgumentMatchers.<Specification<Product>>any(),
            any(Pageable.class)
        );
    }
}
