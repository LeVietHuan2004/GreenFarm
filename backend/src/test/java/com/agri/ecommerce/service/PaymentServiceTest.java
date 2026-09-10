package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.agri.ecommerce.entity.Order;
import com.agri.ecommerce.entity.Payment;
import com.agri.ecommerce.entity.PaymentMethod;
import com.agri.ecommerce.repository.PaymentRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository payments;
    @Mock Order order;

    @BeforeEach void setUp() {
        lenient().when(order.getId()).thenReturn(42L);
        lenient().when(order.getTotalPrice()).thenReturn(new BigDecimal("50000.00"));
        lenient().when(payments.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test void codCreatesPendingPaymentWithoutGatewayUrl() {
        PaymentService service = new PaymentService(payments, "", "", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", "", "http://localhost:3000/payment-result");
        var response = service.createForOrder(order, PaymentMethod.COD, "127.0.0.1");
        assertThat(response.method()).isEqualTo("cod");
        assertThat(response.status()).isEqualTo("pending");
        assertThat(response.referenceCode()).isEqualTo("COD-42");
        assertThat(response.paymentUrl()).isNull();
    }

    @Test void vnpayCannotCreateWhenSandboxIsNotConfigured() {
        PaymentService service = new PaymentService(payments, "", "", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", "", "http://localhost:3000/payment-result");
        assertThat(service.options().vnpayAvailable()).isFalse();
        assertThatThrownBy(() -> service.createForOrder(order, PaymentMethod.VNPAY, "127.0.0.1"))
            .extracting("code").isEqualTo("VNPAY_NOT_CONFIGURED");
    }

    @Test void vnpayCreatesSignedSandboxUrl() {
        PaymentService service = new PaymentService(payments, "TMNCODE", "secret", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html", "https://merchant.example/api/payments/vnpay/return", "http://localhost:3000/payment-result");
        var response = service.createForOrder(order, PaymentMethod.VNPAY, "127.0.0.1");
        assertThat(response.referenceCode()).isEqualTo("VNP-42");
        assertThat(response.paymentUrl()).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?");
        assertThat(response.paymentUrl()).contains("vnp_Amount=5000000", "vnp_SecureHash=");
    }
}
