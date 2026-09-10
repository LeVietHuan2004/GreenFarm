package com.agri.ecommerce.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {
    @Override
    public String convertToDatabaseColumn(PaymentStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String databaseData) {
        return databaseData == null ? null : PaymentStatus.fromValue(databaseData);
    }
}
