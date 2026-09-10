package com.agri.ecommerce.controller.customer;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.ShippingAddressRequest;
import com.agri.ecommerce.dto.response.ShippingAddressResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.ShippingAddressService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping-addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class ShippingAddressController {
    private final ShippingAddressService service;
    public ShippingAddressController(ShippingAddressService service){this.service=service;}
    @GetMapping public ApiResponse<List<ShippingAddressResponse>> findAll(@AuthenticationPrincipal GreenFarmUserDetails user){return ApiResponse.success("Lấy danh sách địa chỉ thành công",service.findAll(user.userId()));}
    @PostMapping public ApiResponse<ShippingAddressResponse> create(@AuthenticationPrincipal GreenFarmUserDetails user,@Valid @RequestBody ShippingAddressRequest request){return ApiResponse.success("Thêm địa chỉ thành công",service.create(user.userId(),request));}
    @PutMapping("/{id}") public ApiResponse<ShippingAddressResponse> update(@AuthenticationPrincipal GreenFarmUserDetails user,@PathVariable Long id,@Valid @RequestBody ShippingAddressRequest request){return ApiResponse.success("Cập nhật địa chỉ thành công",service.update(user.userId(),id,request));}
    @PatchMapping("/{id}/default") public ApiResponse<ShippingAddressResponse> setDefault(@AuthenticationPrincipal GreenFarmUserDetails user,@PathVariable Long id){return ApiResponse.success("Đã đặt làm địa chỉ mặc định",service.setDefault(user.userId(),id));}
    @DeleteMapping("/{id}") public ApiResponse<List<ShippingAddressResponse>> remove(@AuthenticationPrincipal GreenFarmUserDetails user,@PathVariable Long id){return ApiResponse.success("Xóa địa chỉ thành công",service.remove(user.userId(),id));}
}
