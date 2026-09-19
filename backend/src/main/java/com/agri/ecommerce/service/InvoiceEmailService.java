package com.agri.ecommerce.service;

import com.agri.ecommerce.entity.Order;
import com.agri.ecommerce.entity.OrderItem;
import com.agri.ecommerce.entity.Payment;
import com.agri.ecommerce.entity.PaymentMethod;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InvoiceEmailService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceEmailService.class);
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("HH:mm, dd/MM/yyyy");

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final String storefrontUrl;

    public InvoiceEmailService(
        @Nullable JavaMailSender mailSender,
        @Value("${app.invoice.email-enabled:false}") boolean enabled,
        @Value("${app.invoice.from:}") String from,
        @Value("${app.storefront-url:http://localhost:3000}") String storefrontUrl
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.storefrontUrl = storefrontUrl.replaceAll("/+$", "");
    }

    /** Sends once after a successfully-created COD order or a completed VNPAY payment. */
    public void sendInvoiceIfEnabled(Payment payment) {
        if (!enabled || mailSender == null || payment.getInvoiceEmailSentAt() != null) return;
        Order order = payment.getOrder();
        String recipient = order == null || order.getUser() == null ? null : order.getUser().getEmail();
        if (!StringUtils.hasText(recipient)) {
            log.warn("Cannot send invoice for order {} because the customer has no email", order == null ? null : order.getId());
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, "UTF-8");
            if (StringUtils.hasText(from)) helper.setFrom(from);
            helper.setTo(recipient.trim());
            helper.setSubject("GreenFarm | Hóa đơn đơn hàng #" + order.getId());
            helper.setText(render(order, payment), true);
            mailSender.send(message);
            payment.setInvoiceEmailSentAt(java.time.LocalDateTime.now());
            log.info("Invoice email sent for order {} to {}", order.getId(), recipient);
        } catch (Exception exception) {
            // Email delivery must not roll back a paid/created order. A later VNPAY callback can retry.
            log.warn("Unable to send invoice email for order {}", order.getId(), exception);
        }
    }

    private String render(Order order, Payment payment) {
        StringBuilder items = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            items.append("<tr><td>").append(escape(item.getProductName())).append("</td><td style=\"text-align:center\">")
                .append(item.getQuantity()).append("</td><td style=\"text-align:right\">").append(money(item.getPrice()))
                .append("</td><td style=\"text-align:right\">").append(money(lineTotal)).append("</td></tr>");
        }
        String paymentStatus = payment.getPaymentMethod() == PaymentMethod.COD ? "Thanh toán khi nhận hàng (COD)" : "Đã thanh toán qua VNPAY";
        String productDiscount = positive(order.getDiscountAmount())
            ? "<div>Giảm giá" + couponSuffix(order.getCouponCode()) + ": <strong style=\"float:right\">−" + money(amountOrZero(order.getDiscountAmount())) + "</strong></div>"
            : "";
        String loyaltyDiscount = positive(order.getLoyaltyDiscountAmount())
            ? "<div>Điểm tích lũy (" + order.getLoyaltyPointsUsed() + " điểm): <strong style=\"float:right\">−" + money(amountOrZero(order.getLoyaltyDiscountAmount())) + "</strong></div>"
            : "";
        String shippingDiscount = positive(order.getShippingDiscountAmount())
            ? "<div>Giảm phí vận chuyển" + couponSuffix(order.getShippingCouponCode()) + ": <strong style=\"float:right\">−" + money(amountOrZero(order.getShippingDiscountAmount())) + "</strong></div>"
            : "";
        return "<!doctype html><html><body style=\"margin:0;background:#f4f7f4;font-family:Arial,sans-serif;color:#20372a\">"
            + "<main style=\"max-width:680px;margin:24px auto;background:#fff;border:1px solid #dfe8e1;border-radius:12px;overflow:hidden\">"
            + "<header style=\"padding:26px 30px;background:#176b3a;color:#fff\"><strong style=\"font-size:22px\">GreenFarm</strong><p style=\"margin:8px 0 0\">Hóa đơn đơn hàng #" + order.getId() + "</p></header>"
            + "<section style=\"padding:26px 30px\"><p>Xin chào " + escape(order.getUser().getName()) + ",</p>"
            + "<p>Cảm ơn bạn đã mua sắm. Đây là hóa đơn cho đơn hàng được tạo lúc " + order.getCreatedAt().format(DATE_TIME) + ".</p>"
            + "<table style=\"width:100%;border-collapse:collapse;margin:22px 0;font-size:14px\"><thead><tr style=\"background:#f3f7f4\"><th style=\"text-align:left;padding:10px\">Sản phẩm</th><th>SL</th><th style=\"text-align:right\">Đơn giá</th><th style=\"text-align:right\">Thành tiền</th></tr></thead><tbody>"
            + items + "</tbody></table>"
            + "<div style=\"border-top:1px solid #dfe8e1;padding-top:14px;line-height:1.8\"><div>Tạm tính: <strong style=\"float:right\">" + money(order.getSubtotal()) + "</strong></div>"
            + productDiscount
            + loyaltyDiscount
            + "<div>Phí giao hàng: <strong style=\"float:right\">" + money(order.getShippingFee()) + "</strong></div>"
            + shippingDiscount
            + "<div style=\"font-size:18px;font-weight:bold;color:#176b3a\">Tổng cộng: <span style=\"float:right\">" + money(order.getTotalPrice()) + "</span></div></div>"
            + "<p style=\"margin-top:22px\"><strong>Thanh toán:</strong> " + paymentStatus + "</p>"
            + "<p><strong>Giao đến:</strong> " + escape(order.getRecipientName()) + " · " + escape(order.getRecipientPhone()) + "<br>"
            + escape(order.getShippingAddressLine()) + ", " + escape(order.getShippingCity()) + "</p>"
            + "<p style=\"margin-top:25px\"><a href=\"" + storefrontUrl + "/orders/" + order.getId() + "/invoice\" style=\"display:inline-block;padding:11px 16px;background:#176b3a;color:#fff;text-decoration:none;border-radius:6px\">Xem hóa đơn trên GreenFarm</a></p>"
            + "</section></main></body></html>";
    }

    private String couponSuffix(String code) { return StringUtils.hasText(code) ? " (" + escape(code) + ")" : ""; }
    private boolean positive(BigDecimal amount) { return amount != null && amount.signum() > 0; }
    private BigDecimal amountOrZero(BigDecimal amount) { return amount == null ? BigDecimal.ZERO : amount; }
    private String money(BigDecimal amount) { return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + " ₫"; }
    private String escape(String value) { return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
