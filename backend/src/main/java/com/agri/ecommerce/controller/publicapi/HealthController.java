package com.agri.ecommerce.controller.publicapi;

import com.agri.ecommerce.common.base.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success("GreenFarm backend is running", Map.of("status", "ok"));
    }
}
