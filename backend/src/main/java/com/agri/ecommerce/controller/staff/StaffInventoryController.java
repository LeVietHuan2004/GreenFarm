package com.agri.ecommerce.controller.staff;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.InventoryAdjustmentRequest;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/inventory")
@PreAuthorize("hasRole('STAFF')")
public class StaffInventoryController {
    private final InventoryService inventory;
    public StaffInventoryController(InventoryService inventory) { this.inventory = inventory; }
    @GetMapping public ApiResponse<PageResponse<InventoryProductResponse>> list(@RequestParam(required = false) String search, @PageableDefault(size = 20) Pageable page) {
        return ApiResponse.success("Danh sách tồn kho", inventory.inventory(search, null, null, page));
    }
    @GetMapping("/{productId}") public ApiResponse<InventoryDetailResponse> detail(@PathVariable Long productId) {
        return ApiResponse.success("Chi tiết tồn kho", inventory.detail(productId));
    }
    @PostMapping("/adjust") public ApiResponse<InventoryBatchResponse> adjust(@Valid @RequestBody InventoryAdjustmentRequest request, @AuthenticationPrincipal GreenFarmUserDetails user) {
        return ApiResponse.success("Đã điều chỉnh kho", inventory.adjust(request, user.userId()));
    }
}
