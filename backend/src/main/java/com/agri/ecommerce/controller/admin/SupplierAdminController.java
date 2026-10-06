package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.SupplierRequest;
import com.agri.ecommerce.dto.response.PageResponse;
import com.agri.ecommerce.dto.response.SupplierResponse;
import com.agri.ecommerce.service.SupplierService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/suppliers")
@PreAuthorize("hasRole('ADMIN')")
public class SupplierAdminController {
    private final SupplierService suppliers;
    public SupplierAdminController(SupplierService suppliers){this.suppliers=suppliers;}

    @GetMapping
    public ApiResponse<PageResponse<SupplierResponse>> list(@RequestParam(required=false) String search,
            @RequestParam(required=false) String status,
            @PageableDefault(size=20,sort="name",direction=Sort.Direction.ASC) Pageable pageable){
        return ApiResponse.success("Danh sách nhà cung cấp",suppliers.list(search,status,pageable));
    }
    @GetMapping("/{id}")
    public ApiResponse<SupplierResponse> findOne(@PathVariable Long id){return ApiResponse.success("Chi tiết nhà cung cấp",suppliers.findOne(id));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupplierResponse> create(@Valid @RequestBody SupplierRequest request){return ApiResponse.success("Tạo nhà cung cấp thành công",suppliers.create(request));}
    @PutMapping("/{id}")
    public ApiResponse<SupplierResponse> update(@PathVariable Long id,@Valid @RequestBody SupplierRequest request){return ApiResponse.success("Cập nhật nhà cung cấp thành công",suppliers.update(id,request));}
    @DeleteMapping("/{id}")
    public ApiResponse<SupplierResponse> deactivate(@PathVariable Long id){return ApiResponse.success("Đã ngừng sử dụng nhà cung cấp",suppliers.deactivate(id));}
}
