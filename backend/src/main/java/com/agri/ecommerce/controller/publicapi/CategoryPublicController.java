package com.agri.ecommerce.controller.publicapi;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.CategoryResponse;
import com.agri.ecommerce.service.CategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/categories")
public class CategoryPublicController {

    private final CategoryService categoryService;

    public CategoryPublicController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> findCategories() {
        return ApiResponse.success(
            "Lay danh sach danh muc thanh cong",
            categoryService.findPublicCategories()
        );
    }

    @GetMapping("/{slug}")
    public ApiResponse<CategoryResponse> findCategory(@PathVariable String slug) {
        return ApiResponse.success(
            "Lay chi tiet danh muc thanh cong",
            categoryService.findBySlug(slug)
        );
    }
}
