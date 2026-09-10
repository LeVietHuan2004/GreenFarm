package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.CategoryRequest;
import com.agri.ecommerce.dto.response.CategoryResponse;
import com.agri.ecommerce.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
public class CategoryAdminController {

    private final CategoryService categoryService;

    public CategoryAdminController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> findCategories() {
        return ApiResponse.success(
            "Lay danh sach danh muc thanh cong",
            categoryService.findAdminCategories()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("Tao danh muc thanh cong", categoryService.create(request));
    }

    @PutMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> update(
        @PathVariable Long categoryId,
        @Valid @RequestBody CategoryRequest request
    ) {
        return ApiResponse.success(
            "Cap nhat danh muc thanh cong",
            categoryService.update(categoryId, request)
        );
    }

    @DeleteMapping("/{categoryId}")
    public ApiResponse<Void> delete(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return ApiResponse.success("Xoa danh muc thanh cong", null);
    }
}
