package com.agri.ecommerce.config;

import com.agri.ecommerce.common.exception.ApplicationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class GhnProperties {
    private final boolean enabled;
    private final String baseUrl;
    private final String token;
    private final int shopId;
    private final String webhookSecret;
    private final int shippingPaymentType;
    private final String requiredNote;

    public GhnProperties(@Value("${app.ghn.enabled:false}") boolean enabled,
            @Value("${app.ghn.base-url:https://dev-online-gateway.ghn.vn}") String baseUrl,
            @Value("${app.ghn.token:}") String token,
            @Value("${app.ghn.shop-id:0}") int shopId,
            @Value("${app.ghn.webhook-secret:}") String webhookSecret,
            @Value("${app.ghn.shipping-payment-type:1}") int shippingPaymentType,
            @Value("${app.ghn.required-note:CHOXEMHANGKHONGTHU}") String requiredNote) {
        this.enabled = enabled;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.token = token.trim();
        this.shopId = shopId;
        this.webhookSecret = webhookSecret.trim();
        this.shippingPaymentType = shippingPaymentType;
        this.requiredNote = requiredNote;
    }

    public void requireEnabled() {
        if (!enabled) throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_DISABLED", "Giao hàng GHN chưa được bật");
        if (!"https://dev-online-gateway.ghn.vn".equals(baseUrl))
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_STAGING_REQUIRED", "Chỉ hỗ trợ môi trường GHN staging ở giai đoạn thử nghiệm");
        if (token.isBlank() || shopId <= 0 || webhookSecret.isBlank())
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_NOT_CONFIGURED", "Cấu hình GHN staging chưa đầy đủ");
        if (shippingPaymentType != 1)
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_PAYMENT_TYPE_UNSUPPORTED", "GHN hiện chỉ hỗ trợ shop thanh toán phí vận chuyển");
        if (!validRequiredNote())
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "GHN_REQUIRED_NOTE_INVALID", "Ghi chú giao hàng GHN không hợp lệ");
    }
    public boolean enabled(){return enabled;}
    public boolean available(){return enabled && !token.isBlank() && shopId > 0 && !webhookSecret.isBlank()
        && "https://dev-online-gateway.ghn.vn".equals(baseUrl) && shippingPaymentType == 1 && validRequiredNote();}
    private boolean validRequiredNote(){return "KHONGCHOXEMHANG".equals(requiredNote)
        || "CHOXEMHANGKHONGTHU".equals(requiredNote) || "CHOTHUHANG".equals(requiredNote);}
    public String baseUrl(){return baseUrl;}
    public String token(){return token;}
    public int shopId(){return shopId;}
    public String webhookSecret(){return webhookSecret;}
    public int shippingPaymentType(){return shippingPaymentType;}
    public String requiredNote(){return requiredNote;}
}
