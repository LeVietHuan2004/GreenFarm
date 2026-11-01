package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "greenfarm_password_reset_tokens")
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "token_hash", nullable = false, length = 64) private String tokenHash;
    @Column(name = "credential_version", nullable = false) private int credentialVersion;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "used_at") private LocalDateTime usedAt;
    public User getUser() { return user; } public void setUser(User value) { user = value; }
    public void setTokenHash(String value) { tokenHash = value; }
    public int getCredentialVersion() { return credentialVersion; } public void setCredentialVersion(int value) { credentialVersion = value; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; } public void setExpiresAt(LocalDateTime value) { expiresAt = value; }
    public LocalDateTime getUsedAt() { return usedAt; } public void setUsedAt(LocalDateTime value) { usedAt = value; }
}
