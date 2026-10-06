package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "revoked_at") private LocalDateTime revokedAt;
    @Column(name = "replaced_by_hash", length = 64) private String replacedByHash;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void prePersist(){if(createdAt==null)createdAt=LocalDateTime.now();}
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User value){user=value;}
    public String getTokenHash(){return tokenHash;} public void setTokenHash(String value){tokenHash=value;}
    public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime value){expiresAt=value;}
    public LocalDateTime getRevokedAt(){return revokedAt;} public void setRevokedAt(LocalDateTime value){revokedAt=value;}
    public String getReplacedByHash(){return replacedByHash;} public void setReplacedByHash(String value){replacedByHash=value;}
}
