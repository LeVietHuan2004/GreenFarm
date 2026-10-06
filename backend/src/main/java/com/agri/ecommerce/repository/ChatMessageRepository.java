package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop20ByUser_IdOrderByCreatedAtDesc(Long userId);
    List<ChatMessage> findTop20ByGuestTokenHashOrderByCreatedAtDesc(String guestTokenHash);
}
