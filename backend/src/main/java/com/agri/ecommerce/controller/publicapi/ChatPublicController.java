package com.agri.ecommerce.controller.publicapi;

import com.agri.ecommerce.common.base.ApiResponse;
import com.agri.ecommerce.dto.request.ChatSendRequest;
import com.agri.ecommerce.dto.response.ChatConversationResponse;
import com.agri.ecommerce.dto.response.ChatMessageResponse;
import com.agri.ecommerce.security.GreenFarmUserDetails;
import com.agri.ecommerce.service.ChatbotService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/chat")
public class ChatPublicController {
    private final ChatbotService chatbot;
    public ChatPublicController(ChatbotService chatbot){this.chatbot=chatbot;}
    @GetMapping("/messages")
    public ApiResponse<List<ChatMessageResponse>> history(@AuthenticationPrincipal GreenFarmUserDetails user,
                                                          @RequestParam(required = false) String guestToken) {
        return ApiResponse.success("Lấy lịch sử hội thoại thành công", chatbot.history(user, guestToken));
    }
    @PostMapping("/messages")
    public ApiResponse<ChatConversationResponse> send(@AuthenticationPrincipal GreenFarmUserDetails user,
                                                       @Valid @RequestBody ChatSendRequest request) {
        return ApiResponse.success("Trợ lý GreenFarm đã phản hồi", chatbot.send(user, request.guestToken(), request.message()));
    }
}
