package com.agri.ecommerce.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ProductStatusConverter implements AttributeConverter<ProductStatus, String> {

    @Override
    public String convertToDatabaseColumn(ProductStatus status) {
        return status == null ? null : status.getDatabaseValue();
    }

    @Override
    public ProductStatus convertToEntityAttribute(String value) {
        return value == null ? null : ProductStatus.from(value);
    }
}
