package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.entity.PasswordResetToken;
import com.agri.ecommerce.entity.UserStatus;
import com.agri.ecommerce.repository.PasswordResetTokenRepository;
import com.agri.ecommerce.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final RefreshTokenService refreshTokens;
    private final JavaMailSender mail;
    private final String from;
    private final String storefrontUrl;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, PasswordEncoder encoder,
        RefreshTokenService refreshTokens, @Nullable JavaMailSender mail,
        @Value("${app.invoice.from:}") String from, @Value("${app.storefront-url:http://localhost:3000}") String storefrontUrl) {
        this.users = users; this.tokens = tokens; this.encoder = encoder; this.refreshTokens = refreshTokens;
        this.mail = mail; this.from = from; this.storefrontUrl = storefrontUrl.replaceAll("/+$", "");
    }

    @Transactional
    public void request(String email) {
        if (mail == null) throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_UNAVAILABLE", "Chức năng gửi email chưa được cấu hình");
        var found = users.findByEmailIgnoreCase(email.trim());
        if (found.isEmpty()) return;
        var user = users.findByIdForCommerceUpdate(found.get().getId()).orElseThrow();
        if (user.getStatus() != UserStatus.ACTIVE) return;
        var now = LocalDateTime.now();
        if (tokens.existsByUser_IdAndCreatedAtAfter(user.getId(), now.minusMinutes(1))) return;
        tokens.invalidateAll(user.getId(), now);
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var token = new PasswordResetToken(); token.setUser(user); token.setTokenHash(hash(raw));
        token.setCredentialVersion(user.getCredentialVersion()); token.setCreatedAt(now); token.setExpiresAt(now.plusMinutes(30));
        tokens.save(token);
        try {
            var message = new SimpleMailMessage();
            if (StringUtils.hasText(from)) message.setFrom(from);
            message.setTo(user.getEmail()); message.setSubject("GreenFarm - Khôi phục mật khẩu");
            message.setText("Bạn đã yêu cầu đặt lại mật khẩu GreenFarm. Liên kết có hiệu lực trong 30 phút và chỉ dùng một lần:\n\n"
                + storefrontUrl + "/reset-password#token=" + raw + "\n\nNếu bạn không yêu cầu, hãy bỏ qua email này.");
            mail.send(message);
        } catch (Exception exception) {
            token.setUsedAt(now);
            log.warn("Unable to send password reset email");
        }
    }

    @Transactional
    public void reset(String raw, String password) {
        String hash = hash(raw);
        var candidate = tokens.findByTokenHash(hash).orElseThrow(this::invalid);
        var user = users.findByIdForCommerceUpdate(candidate.getUser().getId()).orElseThrow(this::invalid);
        var token = tokens.findLockedByHash(hash).orElseThrow(this::invalid);
        var now = LocalDateTime.now();
        if (token.getUsedAt() != null || !token.getExpiresAt().isAfter(now)
            || token.getCredentialVersion() != user.getCredentialVersion() || user.getStatus() != UserStatus.ACTIVE) throw invalid();
        user.setPassword(encoder.encode(password)); user.incrementCredentialVersion();
        user.setFailedLoginAttempts(0); user.setLockedUntil(null);
        tokens.invalidateAll(user.getId(), now);
        refreshTokens.revokeAll(user.getId());
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    private ApplicationException invalid() { return new ApplicationException(HttpStatus.BAD_REQUEST, "RESET_TOKEN_INVALID", "Liên kết không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu liên kết mới."); }
}
