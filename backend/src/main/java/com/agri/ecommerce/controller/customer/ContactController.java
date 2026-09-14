package com.agri.ecommerce.controller.customer;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.response.ContactResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.ContactService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contacts")
public class ContactController {
    private final ContactService service;
    public ContactController(ContactService service){this.service=service;}
    @GetMapping("/mine") public ApiResponse<List<ContactResponse>> findMine(@AuthenticationPrincipal GreenFarmUserDetails user){return ApiResponse.success("Lấy liên hệ của bạn thành công",service.findMine(user.userId()));}
}
