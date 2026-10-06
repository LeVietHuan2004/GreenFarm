package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.entity.RefreshToken;
import com.agri.ecommerce.entity.User;
import com.agri.ecommerce.entity.UserStatus;
import com.agri.ecommerce.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository tokens; private final long expirationDays; private final SecureRandom random=new SecureRandom();
    public RefreshTokenService(RefreshTokenRepository tokens,@Value("${app.jwt.refresh-expiration-days:30}") long expirationDays){this.tokens=tokens;this.expirationDays=expirationDays;}
    @Transactional public String issue(User user){String raw=randomToken();save(user,raw);return raw;}
    @Transactional public Rotation rotate(String raw){String hash=hash(raw);RefreshToken current=tokens.findByHashForUpdate(hash).orElseThrow(this::invalid);User user=current.getUser();LocalDateTime now=LocalDateTime.now();
        if(current.getRevokedAt()!=null){tokens.revokeAllByUserId(user.getId());throw invalid();}
        if(current.getExpiresAt().isBefore(now)||user.getStatus()!=UserStatus.ACTIVE){current.setRevokedAt(now);throw invalid();}
        String nextRaw=randomToken();String nextHash=hash(nextRaw);current.setRevokedAt(now);current.setReplacedByHash(nextHash);tokens.save(current);save(user,nextRaw);return new Rotation(user,nextRaw);
    }
    @Transactional public void revoke(String raw){if(raw==null||raw.isBlank())return;tokens.findByHashForUpdate(hash(raw)).ifPresent(token->{if(token.getRevokedAt()==null){token.setRevokedAt(LocalDateTime.now());tokens.save(token);}});}
    @Transactional public void revokeAll(Long userId){tokens.revokeAllByUserId(userId);}
    private void save(User user,String raw){RefreshToken token=new RefreshToken();token.setUser(user);token.setTokenHash(hash(raw));token.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));tokens.save(token);}
    private String randomToken(){byte[] bytes=new byte[48];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    private String hash(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception exception){throw new IllegalStateException(exception);}}
    private ApplicationException invalid(){return new ApplicationException(HttpStatus.UNAUTHORIZED,"INVALID_REFRESH_TOKEN","Phiên đăng nhập không hợp lệ hoặc đã hết hạn");}
    public record Rotation(User user,String refreshToken){}
}
