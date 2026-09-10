package com.agri.ecommerce.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {
    @Override
    public String convertToDatabaseColumn(PaymentMethod attribute) {
        return attribute == null ? null : attribute.getDatabaseValue();
    }

    @Override
    public PaymentMethod convertToEntityAttribute(String databaseData) {
        return databaseData == null ? null : PaymentMethod.fromDatabaseValue(databaseData);
    }
}
