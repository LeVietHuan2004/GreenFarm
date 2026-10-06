package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.RefundRequestDecision;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.dto.response.RefundRequestResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.RefundRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/refund-requests")
@PreAuthorize("hasRole('ADMIN')")
public class RefundRequestAdminController {
    private final RefundRequestService refunds;
    public RefundRequestAdminController(RefundRequestService refunds){this.refunds=refunds;}
    @GetMapping public ApiResponse<PageResponse<RefundRequestResponse>> findAll(@RequestParam(required=false) String status,@PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC) Pageable pageable){return ApiResponse.success("Lay danh sach yeu cau hoan tien",refunds.findAll(status,pageable));}
    @PatchMapping("/{id}/approve") public ApiResponse<RefundRequestResponse> approve(@PathVariable Long id,@Valid @RequestBody RefundRequestDecision request,@AuthenticationPrincipal GreenFarmUserDetails user,HttpServletRequest servletRequest){return ApiResponse.success("Yeu cau hoan tien da duoc duyet",refunds.approve(id,request.note(),user.getUsername(),clientIp(servletRequest)));}
    @PatchMapping("/{id}/reject") public ApiResponse<RefundRequestResponse> reject(@PathVariable Long id,@Valid @RequestBody RefundRequestDecision request,@AuthenticationPrincipal GreenFarmUserDetails user){return ApiResponse.success("Yeu cau hoan tien da bi tu choi",refunds.reject(id,request.note(),user.getUsername()));}
    private String clientIp(HttpServletRequest request){String forwarded=request.getHeader("X-Forwarded-For");return forwarded==null||forwarded.isBlank()?request.getRemoteAddr():forwarded.split(",")[0].trim();}
}
