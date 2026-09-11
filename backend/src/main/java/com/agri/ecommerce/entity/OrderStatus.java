package com.agri.ecommerce.entity;

public enum OrderStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    READY_FOR_DELIVERY("ready_for_delivery"),
    OUT_FOR_DELIVERY("out_for_delivery"),
    DELIVERED("delivered"),
    DELIVERY_FAILED("delivery_failed"),
    COMPLETED("completed"),
    CANCELED("canceled");

    private final String value;

    OrderStatus(String value) { this.value = value; }
    public String getValue() { return value; }

    public static OrderStatus fromValue(String value) {
        for (OrderStatus status : values()) if (status.value.equalsIgnoreCase(value)) return status;
        throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ: " + value);
    }
}
