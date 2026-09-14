package com.agri.ecommerce.controller.publicapi;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.ContactRequest;
import com.agri.ecommerce.dto.response.ContactResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/contacts")
public class ContactPublicController {
    private final ContactService service;
    public ContactPublicController(ContactService service) { this.service = service; }
    @PostMapping public ApiResponse<ContactResponse> create(@AuthenticationPrincipal GreenFarmUserDetails user, @Valid @RequestBody ContactRequest request) { return ApiResponse.success("Đã gửi yêu cầu hỗ trợ", service.create(user == null ? null : user.userId(), request)); }
}
