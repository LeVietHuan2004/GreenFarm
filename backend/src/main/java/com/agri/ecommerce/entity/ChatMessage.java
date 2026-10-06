package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(name = "guest_token_hash", length = 64) private String guestTokenHash;
    @Convert(converter = ChatMessageSenderConverter.class) @Column(nullable = false, length = 20) private ChatMessageSender sender;
    @Column(name = "message", nullable = false, columnDefinition = "text") private String content;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void created(){createdAt=createdAt==null?LocalDateTime.now():createdAt;}
    public Long getId(){return id;}
    public User getUser(){return user;} public void setUser(User value){user=value;}
    public String getGuestTokenHash(){return guestTokenHash;} public void setGuestTokenHash(String value){guestTokenHash=value;}
    public ChatMessageSender getSender(){return sender;} public void setSender(ChatMessageSender value){sender=value;}
    public String getContent(){return content;} public void setContent(String value){content=value;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
