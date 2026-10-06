package com.agri.ecommerce.controller.admin;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.StoredFileResponse;
import com.agri.ecommerce.service.ImageStorageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/uploads")
@PreAuthorize("hasAnyAuthority('manage_categories','manage_products')")
public class UploadAdminController {
    private final ImageStorageService images;
    public UploadAdminController(ImageStorageService images) { this.images = images; }

    @PostMapping(value = "/images", consumes = "multipart/form-data")
    public ApiResponse<StoredFileResponse> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("Tải ảnh thành công", new StoredFileResponse(images.store(file, "catalog")));
    }
}
