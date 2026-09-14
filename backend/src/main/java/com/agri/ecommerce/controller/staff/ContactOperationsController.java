package com.agri.ecommerce.controller.staff;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.ContactReplyRequest;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operations/contacts")
@PreAuthorize("hasAuthority('manage_contacts')")
public class ContactOperationsController {
    private final ContactService service;
    public ContactOperationsController(ContactService service) { this.service = service; }
    @GetMapping public ApiResponse<PageResponse<ContactResponse>> findAll(@RequestParam(required=false) String status, @PageableDefault(size=20) Pageable pageable) { return ApiResponse.success("Lấy danh sách liên hệ thành công", service.findAll(status, pageable)); }
    @PatchMapping("/{id}/reply") public ApiResponse<ContactResponse> reply(@AuthenticationPrincipal GreenFarmUserDetails user, @PathVariable Long id, @Valid @RequestBody ContactReplyRequest request) { return ApiResponse.success("Đã phản hồi liên hệ", service.reply(user.userId(), id, request)); }
    @PatchMapping("/{id}/resolve") public ApiResponse<ContactResponse> resolve(@AuthenticationPrincipal GreenFarmUserDetails user, @PathVariable Long id) { return ApiResponse.success("Đã xử lý liên hệ", service.resolve(user.userId(), id)); }
}
