package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.response.PaymentMethodOptionsResponse;
import com.agri.ecommerce.dto.response.PaymentResponse;
import com.agri.ecommerce.entity.Order;
import com.agri.ecommerce.entity.OrderStatus;
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
    private static final String IPN_ORDER_NOT_FOUND = "01";
    private static final String IPN_ALREADY_CONFIRMED = "02";
    private static final String IPN_INVALID_AMOUNT = "04";
    private static final String IPN_INVALID_SIGNATURE = "97";
    private static final String IPN_UNKNOWN_ERROR = "99";
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VIETNAM_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentRepository payments;
    private final OrderLifecycleService orderLifecycle;
    private final InvoiceEmailService invoiceEmails;
    private final CouponEngineService couponEngine;
    private final String tmnCode;
    private final String hashSecret;
    private final String paymentUrl;
    private final String returnUrl;
    private final String frontendResultUrl;

    public PaymentService(
        PaymentRepository payments,
        OrderLifecycleService orderLifecycle,
        InvoiceEmailService invoiceEmails,
        CouponEngineService couponEngine,
        @Value("${app.payment.vnpay.tmn-code:}") String tmnCode,
        @Value("${app.payment.vnpay.hash-secret:}") String hashSecret,
        @Value("${app.payment.vnpay.payment-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String paymentUrl,
        @Value("${app.payment.vnpay.return-url:}") String returnUrl,
        @Value("${app.payment.frontend-result-url:http://localhost:3000/payment-result}") String frontendResultUrl
    ) {
        this.payments = payments;
        this.orderLifecycle = orderLifecycle;
        this.invoiceEmails = invoiceEmails;
        this.couponEngine = couponEngine;
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
        if (method == PaymentMethod.COD) invoiceEmails.sendInvoiceIfEnabled(payment);
        return toResponse(payment, redirectUrl);
    }

    @Transactional(readOnly = true)
    public PaymentResponse findForOrder(Long orderId) {
        return payments.findByOrder_Id(orderId).map(payment -> toResponse(payment, null)).orElse(null);
    }

    @Transactional
    public CallbackOutcome handleVnpayCallback(Map<String, String> parameters) {
        if (!verifySignature(parameters)) {
            return CallbackOutcome.invalid("Chữ ký VNPAY không hợp lệ", null, null, IPN_INVALID_SIGNATURE);
        }
        String reference = parameters.get("vnp_TxnRef");
        if (reference == null || reference.isBlank()) {
            return CallbackOutcome.invalid("Thiếu mã giao dịch", null, null, IPN_UNKNOWN_ERROR);
        }
        Optional<Payment> found = payments.findByReferenceCodeForUpdate(reference);
        if (found.isEmpty()) {
            return CallbackOutcome.invalid("Không tìm thấy giao dịch", null, reference, IPN_ORDER_NOT_FOUND);
        }

        Payment payment = found.get();
        String responseCode = parameters.getOrDefault("vnp_ResponseCode", "");
        String transactionStatus = parameters.getOrDefault("vnp_TransactionStatus", "");
        payment.setGatewayResponseCode(responseCode);
        payment.setGatewayPayload(parameters.toString());
        payment.setTransactionId(parameters.get("vnp_TransactionNo"));

        if (payment.getPaymentMethod() != PaymentMethod.VNPAY) {
            return CallbackOutcome.invalid("Phương thức thanh toán không hợp lệ", payment.getOrder().getId(), reference, IPN_ORDER_NOT_FOUND);
        }
        if (!amountMatches(payment, parameters.get("vnp_Amount"))) {
            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                payment.setStatus(PaymentStatus.FAILED);
                payments.save(payment);
                orderLifecycle.cancelForFailedPayment(payment.getOrder().getId(), "Thanh toán VNPAY sai số tiền");
            }
            return CallbackOutcome.invalid("Số tiền giao dịch không khớp", payment.getOrder().getId(), reference, IPN_INVALID_AMOUNT);
        }

        if (payment.getStatus() == PaymentStatus.COMPLETED) {
            invoiceEmails.sendInvoiceIfEnabled(payment);
            return CallbackOutcome.confirmed("Giao dịch đã được xác nhận", payment.getOrder().getId(), reference, responseCode);
        }
        if (payment.getStatus() == PaymentStatus.FAILED || payment.getOrder().getStatus() == OrderStatus.CANCELED) {
            return CallbackOutcome.invalid("Giao dịch đã kết thúc", payment.getOrder().getId(), reference, IPN_ALREADY_CONFIRMED);
        }
        if (responseCode.isBlank() || transactionStatus.isBlank()) {
            return CallbackOutcome.invalid("Thiếu trạng thái giao dịch", payment.getOrder().getId(), reference, IPN_UNKNOWN_ERROR);
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
        if (successful) couponEngine.markUsed(payment.getOrder());
        if (successful) invoiceEmails.sendInvoiceIfEnabled(payment);
        if (!successful) {
            orderLifecycle.cancelForFailedPayment(payment.getOrder().getId(), "Thanh toán VNPAY không thành công");
        }
        return new CallbackOutcome(successful, true, successful ? "Thanh toán thành công" : "Thanh toán không thành công",
            payment.getOrder().getId(), reference, responseCode, SUCCESS_CODE);
    }

    public Map<String, String> ipnResponse(Map<String, String> parameters) {
        CallbackOutcome outcome = handleVnpayCallback(parameters);
        return Map.of("RspCode", outcome.ipnCode(), "Message", outcome.valid() ? "Confirm Success" : outcome.message());
    }

    @Transactional
    public int expirePendingVnpayPayments() {
        LocalDateTime now = LocalDateTime.now(VIETNAM_TIME_ZONE);
        int expired = 0;
        for (Long paymentId : payments.findExpiredIds(PaymentMethod.VNPAY, PaymentStatus.PENDING, now)) {
            Payment payment = payments.findByIdForUpdate(paymentId).orElse(null);
            if (payment == null || payment.getStatus() != PaymentStatus.PENDING
                || payment.getExpiresAt() == null || payment.getExpiresAt().isAfter(now)) {
                continue;
            }
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponseCode("EXPIRED");
            payments.save(payment);
            orderLifecycle.cancelForFailedPayment(payment.getOrder().getId(), "Thanh toán VNPAY đã hết hạn");
            expired++;
        }
        return expired;
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

    public record CallbackOutcome(
        boolean success,
        boolean valid,
        String message,
        Long orderId,
        String referenceCode,
        String responseCode,
        String ipnCode
    ) {
        static CallbackOutcome invalid(String message, Long orderId, String referenceCode, String ipnCode) {
            return new CallbackOutcome(false, false, message, orderId, referenceCode, null, ipnCode);
        }
        static CallbackOutcome confirmed(String message, Long orderId, String referenceCode, String responseCode) {
            return new CallbackOutcome(true, true, message, orderId, referenceCode, responseCode, IPN_ALREADY_CONFIRMED);
        }
    }
}
