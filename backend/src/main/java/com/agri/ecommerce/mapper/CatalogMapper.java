package com.agri.ecommerce.mapper;

import com.agri.ecommerce.dto.response.CategoryResponse;
import com.agri.ecommerce.dto.response.CategorySummaryResponse;
import com.agri.ecommerce.dto.response.ProductImageResponse;
import com.agri.ecommerce.dto.response.ProductResponse;
import com.agri.ecommerce.entity.Category;
import com.agri.ecommerce.entity.Product;

public final class CatalogMapper {

    private CatalogMapper() {
    }

    public static CategoryResponse toCategoryResponse(Category category, long productCount) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getNameEn(),
            category.getSlug(),
            category.getDescription(),
            category.getDescriptionEn(),
            category.getImage(),
            productCount,
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }

    public static ProductResponse toProductResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getNameEn(),
            product.getSlug(),
            new CategorySummaryResponse(category.getId(), category.getName(), category.getSlug()),
            product.getDescription(),
            product.getDescriptionEn(),
            product.getPrice(),
            product.getStock(),
            product.getStatus().getDatabaseValue(),
            product.getUnit(),
            product.getUnitEn(),
            product.getImages().stream()
                .filter(image -> image.getImage() != null && !image.getImage().isBlank())
                .map(image -> new ProductImageResponse(image.getId(), image.getImage()))
                .toList(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
