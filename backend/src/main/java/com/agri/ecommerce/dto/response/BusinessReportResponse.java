package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BusinessReportResponse(
    LocalDate from,
    LocalDate to,
    BigDecimal netRevenue,
    long paidOrders,
    BigDecimal averageOrderValue,
    List<DailyRevenue> dailyRevenue,
    List<TopProduct> topProducts,
    List<OrderStatusCount> orderStatuses,
    long newCustomers,
    int lowStockThreshold,
    long lowStockCount,
    List<LowStockProduct> lowStockProducts,
    List<CouponPerformance> couponPerformance
) {
    public record DailyRevenue(LocalDate date, BigDecimal revenue, long orders) {}
    public record TopProduct(Long productId, String productName, long quantity, BigDecimal revenue) {}
    public record OrderStatusCount(String status, long count) {}
    public record LowStockProduct(Long productId, String productName, int stock) {}
    public record CouponPerformance(Long couponId, String code, String couponType, long uses, BigDecimal discountAmount) {}
}
