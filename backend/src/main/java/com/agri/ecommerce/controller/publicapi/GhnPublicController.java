package com.agri.ecommerce.controller.publicapi;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.GhnLocationResponse;
import com.agri.ecommerce.service.GhnLocationService;
import com.agri.ecommerce.config.GhnProperties;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/ghn")
public class GhnPublicController {
    private final GhnLocationService locations;
    private final GhnProperties properties;
    public GhnPublicController(GhnLocationService locations, GhnProperties properties) {
        this.locations = locations; this.properties = properties;
    }
    @GetMapping("/status")
    public ApiResponse<Map<String,Boolean>> status() {
        return ApiResponse.success("Trạng thái GHN", Map.of("available", properties.available()));
    }
    @GetMapping("/provinces")
    public ApiResponse<List<GhnLocationResponse>> provinces() {
        return ApiResponse.success("Lấy tỉnh/thành GHN thành công", locations.provinces());
    }
    @GetMapping("/wards")
    public ApiResponse<List<GhnLocationResponse>> wards(@RequestParam int provinceId) {
        return ApiResponse.success("Lấy phường/xã GHN thành công", locations.wards(provinceId));
    }
}
