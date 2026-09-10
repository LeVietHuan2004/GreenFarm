package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.response.PaymentMethodOptionsResponse;
import com.agri.ecommerce.dto.response.PaymentResponse;
import com.agri.ecommerce.entity.Order;
import com.agri.ecommerce.entity.Payment;
import com.agri.ecommerce.entity.PaymentMethod;
import com.agri.ecommerce.entity.PaymentStatus;
import com.agri.ecommerce.repository.PaymentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PaymentService {
    private static final String VNPAY_VERSION = "2.1.0";
    private static final String SUCCESS_CODE = "00";
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentRepository payments;
    private final String tmnCode;
    private final String hashSecret;
    private final String paymentUrl;
    private final String returnUrl;
    private final String frontendResultUrl;

    public PaymentService(
        PaymentRepository payments,
        @Value("${app.payment.vnpay.tmn-code:}") String tmnCode,
        @Value("${app.payment.vnpay.hash-secret:}") String hashSecret,
        @Value("${app.payment.vnpay.payment-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String paymentUrl,
        @Value("${app.payment.vnpay.return-url:}") String returnUrl,
        @Value("${app.payment.frontend-result-url:http://localhost:3000/payment-result}") String frontendResultUrl
    ) {
        this.payments = payments;
        this.tmnCode = tmnCode;
        this.hashSecret = hashSecret;
        this.paymentUrl = paymentUrl;
        this.returnUrl = returnUrl;
        this.frontendResultUrl = frontendResultUrl;
    }

    public PaymentMethodOptionsResponse options() {
        return new PaymentMethodOptionsResponse(true, isVnpayAvailable());
    }

    @Transactional
    public PaymentResponse createForOrder(Order order, PaymentMethod method, String clientIp) {
        if (method == PaymentMethod.PAYPAL) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PAYMENT_METHOD", "Phương thức thanh toán chưa được hỗ trợ");
        }
        if (method == PaymentMethod.VNPAY && !isVnpayAvailable()) {
            throw new ApplicationException(HttpStatus.SERVICE_UNAVAILABLE, "VNPAY_NOT_CONFIGURED", "VNPAY sandbox chưa được cấu hình");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(method);
        payment.setAmount(order.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);
        if (method == PaymentMethod.VNPAY) {
            payment.setExpiresAt(LocalDateTime.now(VIETNAM_TIME_ZONE).plusMinutes(15));
        }
        payment = payments.save(payment);
        payment.setReferenceCode((method == PaymentMethod.COD ? "COD" : "VNP") + "-" + order.getId());
        payment = payments.save(payment);

        String redirectUrl = method == PaymentMethod.VNPAY ? buildVnpayUrl(payment, clientIp) : null;
        return toResponse(payment, redirectUrl);
    }

    @Transactional(readOnly = true)
    public PaymentResponse findForOrder(Long orderId) {
        return payments.findByOrder_Id(orderId).map(payment -> toResponse(payment, null)).orElse(null);
    }

    @Transactional
    public CallbackOutcome handleVnpayCallback(Map<String, String> parameters) {
        if (!verifySignature(parameters)) {
            return CallbackOutcome.invalid("Chữ ký VNPAY không hợp lệ", null, null);
        }
        String reference = parameters.get("vnp_TxnRef");
        Optional<Payment> found = payments.findByReferenceCodeForUpdate(reference);
        if (found.isEmpty()) {
            return CallbackOutcome.invalid("Không tìm thấy giao dịch", null, reference);
        }

        Payment payment = found.get();
        String responseCode = parameters.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = parameters.getOrDefault("vnp_TransactionStatus", "");
        payment.setGatewayResponseCode(responseCode);
        payment.setGatewayPayload(parameters.toString());
        payment.setTransactionId(parameters.get("vnp_TransactionNo"));

        if (payment.getPaymentMethod() != PaymentMethod.VNPAY || !amountMatches(payment, parameters.get("vnp_Amount"))) {
            payment.setStatus(PaymentStatus.FAILED);
            payments.save(payment);
            return CallbackOutcome.invalid("Dữ liệu giao dịch không hợp lệ", payment.getOrder().getId(), reference);
        }

        boolean successful = SUCCESS_CODE.equals(responseCode) && SUCCESS_CODE.equals(transactionStatus);
        if (successful) {
            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now(VIETNAM_TIME_ZONE));
            }
        } else if (payment.getStatus() != PaymentStatus.COMPLETED) {
            payment.setStatus(PaymentStatus.FAILED);
        }
        payments.save(payment);
        return new CallbackOutcome(successful, successful ? "Thanh toán thành công" : "Thanh toán không thành công",
            payment.getOrder().getId(), reference, responseCode);
    }

    public Map<String, String> ipnResponse(Map<String, String> parameters) {
        CallbackOutcome outcome = handleVnpayCallback(parameters);
        if (!outcome.valid()) {
            return Map.of("RspCode", "97", "Message", outcome.message());
        }
        return Map.of("RspCode", SUCCESS_CODE, "Message", "Confirm Success");
    }

    public String resultRedirectUrl(CallbackOutcome outcome) {
        return UriComponentsBuilder.fromUriString(frontendResultUrl)
            .queryParam("success", outcome.success())
            .queryParam("message", outcome.message())
            .queryParamIfPresent("orderId", Optional.ofNullable(outcome.orderId()))
            .build()
            .encode()
            .toUriString();
    }

    private boolean isVnpayAvailable() {
        return !tmnCode.isBlank() && !hashSecret.isBlank() && !returnUrl.isBlank();
    }

    private String buildVnpayUrl(Payment payment, String clientIp) {
        TreeMap<String, String> parameters = new TreeMap<>();
        parameters.put("vnp_Version", VNPAY_VERSION);
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", tmnCode);
        parameters.put("vnp_Amount", payment.getAmount().movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).toPlainString());
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", payment.getReferenceCode());
        parameters.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getOrder().getId());
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_ReturnUrl", returnUrl);
        parameters.put("vnp_IpAddr", clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp);
        parameters.put("vnp_CreateDate", LocalDateTime.now(VIETNAM_TIME_ZONE).format(VNPAY_DATE));
        parameters.put("vnp_ExpireDate", payment.getExpiresAt().atZone(VIETNAM_TIME_ZONE).format(VNPAY_DATE));
        String query = toQuery(parameters);
        return paymentUrl + "?" + query + "&vnp_SecureHash=" + hmacSha512(query);
    }

    private boolean verifySignature(Map<String, String> source) {
        if (!isVnpayAvailable() || source.get("vnp_SecureHash") == null) {
            return false;
        }
        TreeMap<String, String> parameters = new TreeMap<>();
        source.forEach((key, value) -> {
            if (!"vnp_SecureHash".equals(key) && !"vnp_SecureHashType".equals(key) && value != null && !value.isBlank()) {
                parameters.put(key, value);
            }
        });
        return constantTimeEquals(hmacSha512(toQuery(parameters)), source.get("vnp_SecureHash"));
    }

    private boolean amountMatches(Payment payment, String amount) {
        try {
            return payment.getAmount().movePointRight(2).setScale(0, RoundingMode.UNNECESSARY)
                .compareTo(new BigDecimal(amount)) == 0;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String toQuery(Map<String, String> parameters) {
        return parameters.entrySet().stream()
            .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
            .reduce((left, right) -> left + "&" + right)
            .orElse("");
    }

    private String hmacSha512(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) hash.append(String.format("%02x", item));
            return hash.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Không thể tạo chữ ký VNPAY", exception);
        }
    }

    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private boolean constantTimeEquals(String left, String right) {
        if (right == null || left.length() != right.length()) return false;
        int difference = 0;
        for (int i = 0; i < left.length(); i++) difference |= left.charAt(i) ^ right.charAt(i);
        return difference == 0;
    }

    private PaymentResponse toResponse(Payment payment, String paymentUrl) {
        return new PaymentResponse(payment.getId(), payment.getPaymentMethod().getApiValue(), payment.getStatus().getValue(),
            payment.getAmount(), payment.getReferenceCode(), payment.getTransactionId(), payment.getPaidAt(), paymentUrl);
    }

    public record CallbackOutcome(boolean success, String message, Long orderId, String referenceCode, String responseCode) {
        static CallbackOutcome invalid(String message, Long orderId, String referenceCode) {
            return new CallbackOutcome(false, message, orderId, referenceCode, null);
        }
        boolean valid() { return responseCode != null || orderId != null; }
    }
}
