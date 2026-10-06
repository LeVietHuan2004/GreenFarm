package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.InventoryAdjustmentRequest;
import com.agri.ecommerce.dto.request.InventoryImportRequest;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.InventoryService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryAdminController {
    private final InventoryService inventory;
    public InventoryAdminController(InventoryService inventory){this.inventory=inventory;}

    @GetMapping("/summary")
    public ApiResponse<InventorySummaryResponse> summary(){return ApiResponse.success("Tổng quan kho",inventory.summary());}

    @GetMapping
    public ApiResponse<PageResponse<InventoryProductResponse>> list(@RequestParam(required=false) String search,
            @RequestParam(required=false) String status,@RequestParam(required=false) String expiry,
            @PageableDefault(size=20) Pageable pageable){
        return ApiResponse.success("Danh sách tồn kho",inventory.inventory(search,status,expiry,pageable));
    }

    @GetMapping("/batches")
    public ApiResponse<PageResponse<InventoryBatchResponse>> batches(@RequestParam(required=false) Long productId,
            @RequestParam(required=false) String search,@RequestParam(required=false) String expiry,
            @PageableDefault(size=20,sort="importedAt",direction=Sort.Direction.DESC) Pageable pageable){
        return ApiResponse.success("Danh sách lô hàng",inventory.batchPage(productId,search,expiry,pageable));
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<InventoryTransactionResponse>> transactions(@RequestParam(required=false) Long productId,
            @RequestParam(required=false) String type,@RequestParam(required=false) String search,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size=20,sort="createdAt",direction=Sort.Direction.DESC) Pageable pageable){
        return ApiResponse.success("Lịch sử kho",inventory.transactionPage(productId,type,search,from,to,pageable));
    }

    @GetMapping("/{productId}")
    public ApiResponse<InventoryDetailResponse> detail(@PathVariable Long productId){return ApiResponse.success("Chi tiết tồn kho",inventory.detail(productId));}

    @PostMapping("/import") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryBatchResponse> importBatch(@Valid @RequestBody InventoryImportRequest request,
            @AuthenticationPrincipal GreenFarmUserDetails actor){
        return ApiResponse.success("Nhập kho thành công",inventory.importBatch(request,actor.userId()));
    }

    @PostMapping("/adjust")
    public ApiResponse<InventoryBatchResponse> adjust(@Valid @RequestBody InventoryAdjustmentRequest request,
            @AuthenticationPrincipal GreenFarmUserDetails actor){
        return ApiResponse.success("Điều chỉnh kho thành công",inventory.adjust(request,actor.userId()));
    }
}
