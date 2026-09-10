package com.agri.ecommerce.entity;

import java.util.Locale;

public enum PaymentMethod {
    COD("cash"),
    VNPAY("vnpay"),
    PAYPAL("paypal");

    private final String databaseValue;

    PaymentMethod(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String getDatabaseValue() {
        return databaseValue;
    }

    public String getApiValue() {
        return this == COD ? "cod" : databaseValue;
    }

    public static PaymentMethod fromDatabaseValue(String value) {
        for (PaymentMethod method : values()) {
            if (method.databaseValue.equalsIgnoreCase(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ: " + value);
    }

    public static PaymentMethod fromRequestValue(String value) {
        if (value == null || value.isBlank() || "cod".equalsIgnoreCase(value) || "cash".equalsIgnoreCase(value)) {
            return COD;
        }
        return fromDatabaseValue(value.trim().toLowerCase(Locale.ROOT));
    }
}
