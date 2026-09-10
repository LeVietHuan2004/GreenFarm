package com.agri.ecommerce.entity;

import java.util.Arrays;

public enum UserStatus {
    PENDING("pending"),
    ACTIVE("active"),
    BANNED("banned"),
    DELETED("deleted");

    private final String databaseValue;

    UserStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public static UserStatus from(String value) {
        return Arrays.stream(values())
            .filter(status -> status.databaseValue.equalsIgnoreCase(value)
                || status.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Trang thai nguoi dung khong hop le"));
    }
}
