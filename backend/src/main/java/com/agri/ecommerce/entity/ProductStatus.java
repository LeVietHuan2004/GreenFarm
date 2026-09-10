package com.agri.ecommerce.entity;

import java.util.Arrays;

public enum ProductStatus {
    IN_STOCK("in_stock"),
    OUT_OF_STOCK("out_of_stock"),
    HIDDEN("hidden");

    private final String databaseValue;

    ProductStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static ProductStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.databaseValue.equalsIgnoreCase(value)
                || status.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Trang thai san pham khong hop le"));
    }
}
