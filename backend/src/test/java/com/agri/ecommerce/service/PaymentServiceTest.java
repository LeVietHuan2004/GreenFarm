package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.PaymentRepository;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    private static final String SECRET = "test-secret";
    @Mock PaymentRepository payments;
    @Mock OrderLifecycleService orderLifecycle;
    @Mock InvoiceEmailService invoiceEmails;
    @Mock CouponEngineService couponEngine;
    @Mock Order order;
    PaymentService service;

    @BeforeEach void setUp() {
        lenient().when(order.getId()).thenReturn(42L);
        lenient().when(order.getStatus()).thenReturn(OrderStatus.PENDING);
        lenient().when(order.getTotalPrice()).thenReturn(new BigDecimal("50000.00"));
        lenient().when(payments.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
        service = configuredService();
    }

    @Test void codCreatesPendingPaymentWithoutGatewayUrl() {
        var response = service.createForOrder(order, PaymentMethod.COD, "127.0.0.1");
        assertThat(response.method()).isEqualTo("cod");
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.referenceCode()).isEqualTo("COD-42");
        assertThat(response.paymentUrl()).isNull();
        verify(invoiceEmails).sendInvoiceIfEnabled(any(Payment.class));
    }

    @Test void vnpayCannotCreateWhenSandboxIsNotConfigured() {
        PaymentService unavailable = new PaymentService(payments, orderLifecycle, invoiceEmails, couponEngine, "", "", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", "", "http://localhost:3000/payment-result");
        assertThat(unavailable.options().vnpayAvailable()).isFalse();
        assertThatThrownBy(() -> unavailable.createForOrder(order, PaymentMethod.VNPAY, "127.0.0.1"))
            .extracting("code").isEqualTo("VNPAY_NOT_CONFIGURED");
    }

    @Test void vnpayCreatesSignedSandboxUrl() {
        var response = service.createForOrder(order, PaymentMethod.VNPAY, "127.0.0.1");
        assertThat(response.referenceCode()).isEqualTo("VNP-42");
        assertThat(response.paymentUrl()).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?");
        assertThat(response.paymentUrl()).contains("vnp_Amount=5000000", "vnp_SecureHash=");
    }

    @Test void replayCanRecoverPendingVnpayUrlButNotExpiredPayment() {
        Payment payment = pendingPayment();
        payment.setExpiresAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMinutes(10));
        when(payments.findByOrder_Id(42L)).thenReturn(Optional.of(payment));
        String url = service.pendingVnpayUrl(42L, "127.0.0.1");
        assertThat(url).contains("vnp_TxnRef=VNP-42", "vnp_SecureHash=");
        assertThat(service.pendingVnpayUrl(42L, "127.0.0.1")).isEqualTo(url);
        payment.setExpiresAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(1));
        assertThat(service.pendingVnpayUrl(42L, "127.0.0.1")).isNull();
    }

    @Test void successfulCallbackCompletesPayment() {
        Payment payment = pendingPayment();
        when(payments.findByReferenceCodeForUpdate("VNP-42")).thenReturn(Optional.of(payment));
        var outcome = service.handleVnpayCallback(signedParameters("5000000", "00", "00"));
        assertThat(outcome.success()).isTrue();
        assertThat(outcome.valid()).isTrue();
        assertThat(outcome.ipnCode()).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getPaidAt()).isNotNull();
        verify(couponEngine).markUsed(order);
        verify(invoiceEmails).sendInvoiceIfEnabled(payment);
        verify(orderLifecycle, never()).cancelForFailedPayment(any(), any());
    }

    @Test void invalidAmountReturnsCode04AndCancelsOrder() {
        Payment payment = pendingPayment();
        when(payments.findByReferenceCodeForUpdate("VNP-42")).thenReturn(Optional.of(payment));
        Map<String, String> response = service.ipnResponse(signedParameters("4000000", "00", "00"));
        assertThat(response.get("RspCode")).isEqualTo("04");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(orderLifecycle).cancelForFailedPayment(42L, "Thanh toán VNPAY sai số tiền");
    }

    @Test void failedGatewayCallbackCancelsOrderAndConfirmsReceipt() {
        Payment payment = pendingPayment();
        when(payments.findByReferenceCodeForUpdate("VNP-42")).thenReturn(Optional.of(payment));
        Map<String, String> response = service.ipnResponse(signedParameters("5000000", "24", "02"));
        assertThat(response.get("RspCode")).isEqualTo("00");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(orderLifecycle).cancelForFailedPayment(42L, "Thanh toán VNPAY không thành công");
    }

    @Test void invalidSignatureDoesNotTouchPayment() {
        Map<String, String> response = service.ipnResponse(Map.of("vnp_TxnRef", "VNP-42", "vnp_SecureHash", "invalid"));
        assertThat(response.get("RspCode")).isEqualTo("97");
        verifyNoInteractions(orderLifecycle);
        verify(payments, never()).findByReferenceCodeForUpdate(any());
    }

    @Test void duplicateSuccessfulCallbackIsIdempotent() {
        Payment payment = pendingPayment();
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        when(payments.findByReferenceCodeForUpdate("VNP-42")).thenReturn(Optional.of(payment));
        var outcome = service.handleVnpayCallback(signedParameters("5000000", "00", "00"));
        assertThat(outcome.success()).isTrue();
        assertThat(outcome.ipnCode()).isEqualTo("02");
        verify(couponEngine, never()).markUsed(any());
        verify(orderLifecycle, never()).cancelForFailedPayment(any(), any());
    }

    @Test void expiredPaymentIsFailedAndOrderIsCanceled() {
        Payment payment = pendingPayment();
        payment.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(payments.findExpiredIds(eq(PaymentMethod.VNPAY), eq(PaymentStatus.PENDING), any())).thenReturn(List.of(9L));
        when(payments.findByIdForUpdate(9L)).thenReturn(Optional.of(payment));
        assertThat(service.expirePendingVnpayPayments()).isEqualTo(1);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getGatewayResponseCode()).isEqualTo("EXPIRED");
        verify(orderLifecycle).cancelForFailedPayment(42L, "Thanh toán VNPAY đã hết hạn");
    }

    private PaymentService configuredService() {
        return new PaymentService(payments, orderLifecycle, invoiceEmails, couponEngine, "TMNCODE", SECRET,
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", "https://merchant.example/api/payments/vnpay/return",
            "http://localhost:3000/payment-result");
    }

    private Payment pendingPayment() {
        Payment payment = new Payment();
        payment.setOrder(order); payment.setPaymentMethod(PaymentMethod.VNPAY); payment.setReferenceCode("VNP-42");
        payment.setAmount(new BigDecimal("50000.00")); payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    private Map<String, String> signedParameters(String amount, String responseCode, String transactionStatus) {
        TreeMap<String, String> parameters = new TreeMap<>();
        parameters.put("vnp_Amount", amount); parameters.put("vnp_ResponseCode", responseCode);
        parameters.put("vnp_TransactionNo", "123456"); parameters.put("vnp_TransactionStatus", transactionStatus);
        parameters.put("vnp_TxnRef", "VNP-42"); parameters.put("vnp_SecureHash", hmac(toQuery(parameters)));
        return parameters;
    }

    private String toQuery(Map<String, String> parameters) {
        return parameters.entrySet().stream().filter(entry -> !"vnp_SecureHash".equals(entry.getKey()))
            .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
            .reduce((left, right) -> left + "&" + right).orElse("");
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            StringBuilder result = new StringBuilder();
            for (byte item : mac.doFinal(value.getBytes(StandardCharsets.UTF_8))) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
