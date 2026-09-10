package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.RoleResponse;
import com.agri.ecommerce.service.RoleService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RoleAdminController {

    private final RoleService roleService;

    public RoleAdminController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> findRoles() {
        return ApiResponse.success("Lay danh sach vai tro thanh cong", roleService.findAll());
    }
}
