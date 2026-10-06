package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "guest_sessions")
public class GuestSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "consumed_at") private LocalDateTime consumedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "merged_user_id") private User mergedUser;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){var now=LocalDateTime.now();createdAt=createdAt==null?now:createdAt;updatedAt=now;}
    @PreUpdate void preUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public String getTokenHash(){return tokenHash;} public void setTokenHash(String value){tokenHash=value;}
    public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime value){expiresAt=value;}
    public LocalDateTime getConsumedAt(){return consumedAt;} public void setConsumedAt(LocalDateTime value){consumedAt=value;}
    public User getMergedUser(){return mergedUser;} public void setMergedUser(User value){mergedUser=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
