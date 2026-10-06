package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agri.ecommerce.repository.BusinessReportRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessReportServiceTest {
    @Mock BusinessReportRepository repository;

    @Test void combinesPaidSalesOrderStatusesInventoryCustomersAndCoupons() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = LocalDate.of(2026, 9, 30);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        when(repository.totals(start, end)).thenReturn(new Object[]{new BigDecimal("250000"), 2L});
        when(repository.daily(start, end)).thenReturn(List.<Object[]>of(new Object[]{"2026-09-12", new BigDecimal("250000"), 2L}));
        when(repository.topProducts(start, end)).thenReturn(List.<Object[]>of(new Object[]{7L, "Rau sạch", 4L, new BigDecimal("120000")}));
        when(repository.orderStatuses(start, end)).thenReturn(List.<Object[]>of(new Object[]{"completed", 2L}, new Object[]{"pending", 1L}));
        when(repository.newCustomers(start, end)).thenReturn(3L);
        when(repository.lowStockCount(10)).thenReturn(1L);
        when(repository.lowStockProducts(10)).thenReturn(List.<Object[]>of(new Object[]{7L, "Rau sạch", 4}));
        when(repository.couponPerformance(start, end)).thenReturn(List.<Object[]>of(new Object[]{5L, "GREEN10", "ORDER_DISCOUNT", 2L, new BigDecimal("20000")}));

        BusinessReportService service = new BusinessReportService(repository);
        var report = service.report(from, to);

        assertThat(report.netRevenue()).isEqualByComparingTo("250000");
        assertThat(report.averageOrderValue()).isEqualByComparingTo("125000");
        assertThat(report.orderStatuses()).hasSize(8);
        assertThat(report.orderStatuses()).anySatisfy(status -> {
            assertThat(status.status()).isEqualTo("pending");
            assertThat(status.count()).isEqualTo(1);
        });
        assertThat(report.newCustomers()).isEqualTo(3);
        assertThat(report.lowStockCount()).isEqualTo(1);
        assertThat(report.lowStockProducts().getFirst().stock()).isEqualTo(4);
        assertThat(report.couponPerformance().getFirst().discountAmount()).isEqualByComparingTo("20000");
        verify(repository).totals(start, end);

        String csv = new String(service.csv(from, to), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFF").contains("Khách hàng mới", "tồn kho thấp", "GREEN10", "Đơn đã thanh toán");
    }

    @Test void rejectsReversedDateRange() {
        BusinessReportService service = new BusinessReportService(repository);
        assertThatThrownBy(() -> service.report(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)))
            .extracting("code").isEqualTo("INVALID_REPORT_RANGE");
    }
}
