package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.entity.Order;
import com.agri.ecommerce.entity.OrderItem;
import com.agri.ecommerce.entity.Payment;
import com.agri.ecommerce.entity.PaymentMethod;
import com.agri.ecommerce.entity.PaymentStatus;
import com.agri.ecommerce.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

class InvoiceEmailServiceTest {
    @Test
    void sendsHtmlInvoiceOnceAndTracksDelivery() {
        InvoiceEmailSender sender = configuredSender();
        InvoiceEmailService service = new InvoiceEmailService(sender, true, "https://greenfarm.example", 5, 2);
        Payment payment = paymentWithOrder();

        service.sendInvoiceIfEnabled(payment);
        service.sendInvoiceIfEnabled(payment);

        verify(sender, times(1)).send(eq("customer@greenfarm.test"), contains("#12"), contains("Cà chua"), eq("greenfarm-invoice/12"));
        assertThat(payment.getInvoiceEmailSentAt()).isNotNull();
        assertThat(service.status(payment)).isEqualTo("sent");
    }

    @Test
    void leavesPaymentUntouchedWhenEmailIsDisabled() {
        InvoiceEmailSender sender = mock(InvoiceEmailSender.class);
        InvoiceEmailService service = new InvoiceEmailService(sender, false, "https://greenfarm.example", 5, 2);
        Payment payment = paymentWithOrder();

        service.sendInvoiceIfEnabled(payment);

        verifyNoInteractions(sender);
        assertThat(payment.getInvoiceEmailSentAt()).isNull();
        assertThat(service.status(payment)).isEqualTo("disabled");
    }

    @Test void guestInvoiceUsesTheCheckoutEmail() {
        InvoiceEmailSender sender = configuredSender();
        var service = new InvoiceEmailService(sender, true, "https://greenfarm.example", 5, 2);
        Payment payment = paymentWithOrder();
        when(payment.getOrder().getUser()).thenReturn(null);
        when(payment.getOrder().getGuestEmail()).thenReturn("guest@greenfarm.test");
        service.sendInvoiceIfEnabled(payment);
        verify(sender).send(eq("guest@greenfarm.test"), anyString(), contains("/guest-orders"), eq("greenfarm-invoice/12"));
    }

    @Test void unpaidOrFailedVnpayDoesNotSendAnInvoice() {
        InvoiceEmailSender sender = configuredSender();
        var service = new InvoiceEmailService(sender, true, "https://greenfarm.example", 5, 2);
        Payment payment = paymentWithOrder();
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        for (PaymentStatus status : List.of(PaymentStatus.PENDING, PaymentStatus.FAILED, PaymentStatus.REFUNDED)) {
            payment.setStatus(status);
            service.sendInvoiceIfEnabled(payment);
            assertThat(service.status(payment)).isEqualTo("not_applicable");
        }
        verify(sender, never()).send(anyString(), anyString(), anyString(), anyString());
        payment.setStatus(PaymentStatus.COMPLETED);
        service.sendInvoiceIfEnabled(payment);
        assertThat(service.status(payment)).isEqualTo("sent");
    }

    @Test void missingConfigurationDoesNotClaimSentOrConsumeRetries() {
        InvoiceEmailSender sender = mock(InvoiceEmailSender.class);
        var service = new InvoiceEmailService(sender, true, "https://greenfarm.example", 5, 2);
        Payment payment = paymentWithOrder();
        service.sendInvoiceIfEnabled(payment);
        assertThat(service.status(payment)).isEqualTo("not_configured");
        assertThat(payment.getInvoiceEmailAttempts()).isZero();
        assertThat(payment.getInvoiceEmailSentAt()).isNull();
        verify(sender, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test void transientFailureSchedulesRetryAndDuplicateCallbackRespectsBackoff() {
        InvoiceEmailSender sender = configuredSender();
        doThrow(new MailSendException("Resend unavailable")).doNothing()
            .when(sender).send(anyString(), anyString(), anyString(), anyString());
        var service = new InvoiceEmailService(sender, true, "https://greenfarm.example", 5, 2);
        Payment payment = paymentWithOrder();
        service.sendInvoiceIfEnabled(payment);
        assertThat(payment.getInvoiceEmailSentAt()).isNull();
        assertThat(payment.getInvoiceEmailAttempts()).isEqualTo(1);
        assertThat(payment.getInvoiceEmailNextRetryAt()).isAfter(LocalDateTime.now());
        assertThat(service.status(payment)).isEqualTo("retrying");
        service.sendInvoiceIfEnabled(payment);
        verify(sender, times(1)).send(anyString(), anyString(), anyString(), anyString());
        payment.setInvoiceEmailNextRetryAt(LocalDateTime.now().minusSeconds(1));
        service.sendInvoiceIfEnabled(payment);
        verify(sender, times(2)).send(anyString(), anyString(), anyString(), eq("greenfarm-invoice/12"));
        assertThat(service.status(payment)).isEqualTo("sent");
        assertThat(payment.getInvoiceEmailLastError()).isNull();
        assertThat(payment.getInvoiceEmailNextRetryAt()).isNull();
    }

    @Test void exhaustedRetriesStopSending() {
        InvoiceEmailSender sender = configuredSender();
        doThrow(new MailSendException("Provider rejected email")).when(sender)
            .send(anyString(), anyString(), anyString(), anyString());
        var service = new InvoiceEmailService(sender, true, "https://greenfarm.example", 1, 2);
        Payment payment = paymentWithOrder();
        service.sendInvoiceIfEnabled(payment);
        service.sendInvoiceIfEnabled(payment);
        assertThat(service.status(payment)).isEqualTo("failed");
        assertThat(payment.getInvoiceEmailSentAt()).isNull();
        assertThat(payment.getInvoiceEmailNextRetryAt()).isNull();
        verify(sender, times(1)).send(anyString(), anyString(), anyString(), anyString());
    }

    private InvoiceEmailSender configuredSender() {
        var sender = mock(InvoiceEmailSender.class);
        when(sender.configured()).thenReturn(true);
        return sender;
    }

    private Payment paymentWithOrder() {
        User user = new User(); user.setName("Khách hàng"); user.setEmail("customer@greenfarm.test");
        OrderItem item = mock(OrderItem.class);
        when(item.getProductName()).thenReturn("Cà chua"); when(item.getQuantity()).thenReturn(2);
        when(item.getPrice()).thenReturn(new BigDecimal("25000"));
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(12L); when(order.getUser()).thenReturn(user); when(order.getItems()).thenReturn(List.of(item));
        when(order.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 9, 14, 10, 0));
        when(order.getSubtotal()).thenReturn(new BigDecimal("50000")); when(order.getShippingFee()).thenReturn(new BigDecimal("30000"));
        when(order.getDiscountAmount()).thenReturn(BigDecimal.ZERO); when(order.getTotalPrice()).thenReturn(new BigDecimal("80000"));
        when(order.getRecipientName()).thenReturn("Khách hàng"); when(order.getRecipientPhone()).thenReturn("0900000000");
        when(order.getShippingAddressLine()).thenReturn("1 Đường X"); when(order.getShippingCity()).thenReturn("TP.HCM");
        Payment payment = new Payment(); payment.setOrder(order); payment.setPaymentMethod(PaymentMethod.COD); payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }
}
