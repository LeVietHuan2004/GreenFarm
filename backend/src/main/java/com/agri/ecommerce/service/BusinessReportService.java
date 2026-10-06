package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.response.BusinessReportResponse;
import com.agri.ecommerce.entity.OrderStatus;
import com.agri.ecommerce.repository.BusinessReportRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessReportService {
    private static final int LOW_STOCK_THRESHOLD = 10;
    private final BusinessReportRepository reports;
    public BusinessReportService(BusinessReportRepository reports) { this.reports = reports; }

    @Transactional(readOnly = true)
    public BusinessReportResponse report(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(29) : from;
        if (effectiveFrom.isAfter(effectiveTo) || effectiveFrom.isBefore(effectiveTo.minusYears(2)))
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "INVALID_REPORT_RANGE", "Khoảng thời gian báo cáo không hợp lệ");
        LocalDateTime start = effectiveFrom.atStartOfDay();
        LocalDateTime end = effectiveTo.plusDays(1).atStartOfDay();
        Object[] totalRow = reports.totals(start, end);
        if (totalRow.length == 1 && totalRow[0] instanceof Object[] nested) totalRow = nested;
        BigDecimal revenue = decimal(totalRow[0]);
        long orders = number(totalRow[1]);
        BigDecimal average = orders == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
        List<BusinessReportResponse.DailyRevenue> daily = reports.daily(start, end).stream()
            .map(row -> new BusinessReportResponse.DailyRevenue(LocalDate.parse(row[0].toString()), decimal(row[1]), number(row[2]))).toList();
        List<BusinessReportResponse.TopProduct> top = reports.topProducts(start, end).stream()
            .map(row -> new BusinessReportResponse.TopProduct(number(row[0]), String.valueOf(row[1]), number(row[2]), decimal(row[3]))).toList();
        Map<String, Long> statusCounts = reports.orderStatuses(start, end).stream()
            .collect(Collectors.toMap(row -> String.valueOf(row[0]), row -> number(row[1])));
        List<BusinessReportResponse.OrderStatusCount> orderStatuses = java.util.Arrays.stream(OrderStatus.values())
            .map(status -> new BusinessReportResponse.OrderStatusCount(status.getValue(), statusCounts.getOrDefault(status.getValue(), 0L)))
            .toList();
        List<BusinessReportResponse.LowStockProduct> lowStock = reports.lowStockProducts(LOW_STOCK_THRESHOLD).stream()
            .map(row -> new BusinessReportResponse.LowStockProduct(number(row[0]), String.valueOf(row[1]), Math.toIntExact(number(row[2])))).toList();
        List<BusinessReportResponse.CouponPerformance> coupons = reports.couponPerformance(start, end).stream()
            .map(row -> new BusinessReportResponse.CouponPerformance(number(row[0]), String.valueOf(row[1]), String.valueOf(row[2]), number(row[3]), decimal(row[4]))).toList();
        return new BusinessReportResponse(effectiveFrom, effectiveTo, revenue, orders, average, daily, top,
            orderStatuses, reports.newCustomers(start, end), LOW_STOCK_THRESHOLD,
            reports.lowStockCount(LOW_STOCK_THRESHOLD), lowStock, coupons);
    }

    public byte[] csv(LocalDate from, LocalDate to) {
        BusinessReportResponse report = report(from, to);
        StringBuilder csv = new StringBuilder("Từ ngày,Đến ngày,Doanh thu thực nhận,Đơn đã thanh toán,Giá trị đơn trung bình,Khách hàng mới,Sản phẩm tồn kho thấp\r\n");
        csv.append(report.from()).append(',').append(report.to()).append(',').append(report.netRevenue()).append(',')
            .append(report.paidOrders()).append(',').append(report.averageOrderValue()).append(',')
            .append(report.newCustomers()).append(',').append(report.lowStockCount()).append("\r\n\r\n");
        csv.append("Ngày,Doanh thu,Đơn đã thanh toán\r\n");
        report.dailyRevenue().forEach(day -> csv.append(day.date()).append(',').append(day.revenue()).append(',').append(day.orders()).append("\r\n"));
        csv.append("\r\nTrạng thái đơn,Số đơn tạo trong kỳ\r\n");
        report.orderStatuses().forEach(status -> csv.append(status.status()).append(',').append(status.count()).append("\r\n"));
        csv.append("\r\nSản phẩm,Số lượng,Doanh số trước ưu đãi\r\n");
        report.topProducts().forEach(item -> csv.append(escape(item.productName())).append(',').append(item.quantity()).append(',').append(item.revenue()).append("\r\n"));
        csv.append("\r\nSản phẩm tồn kho thấp,Số lượng còn\r\n");
        report.lowStockProducts().forEach(item -> csv.append(escape(item.productName())).append(',').append(item.stock()).append("\r\n"));
        csv.append("\r\nMã coupon,Loại,Lượt dùng trên đơn đã thanh toán,Tổng giảm giá\r\n");
        report.couponPerformance().forEach(item -> csv.append(escape(item.code())).append(',').append(item.couponType()).append(',')
            .append(item.uses()).append(',').append(item.discountAmount()).append("\r\n"));
        return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
    }

    private long number(Object value) { return value == null ? 0 : ((Number)value).longValue(); }
    private BigDecimal decimal(Object value) { return value == null ? BigDecimal.ZERO : value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString()); }
    private String escape(String value) {
        String safe = value.matches("^\\s*[=+@-].*") ? "'" + value : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
