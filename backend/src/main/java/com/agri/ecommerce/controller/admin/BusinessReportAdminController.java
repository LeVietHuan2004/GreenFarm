package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.BusinessReportResponse;
import com.agri.ecommerce.service.BusinessReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class BusinessReportAdminController {
    private final BusinessReportService reports;
    public BusinessReportAdminController(BusinessReportService reports) { this.reports = reports; }

    @GetMapping
    public ApiResponse<BusinessReportResponse> report(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                                                       @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success("Lấy báo cáo kinh doanh thành công", reports.report(from, to));
    }

    @GetMapping(value="/export.csv", produces="text/csv")
    public ResponseEntity<byte[]> export(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=greenfarm-report.csv")
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(reports.csv(from, to));
    }
}
