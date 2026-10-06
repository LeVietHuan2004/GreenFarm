package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.entity.Order;
import com.agri.ecommerce.entity.OrderItem;
import com.agri.ecommerce.entity.Payment;
import com.agri.ecommerce.entity.PaymentMethod;
import com.agri.ecommerce.entity.User;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class InvoiceEmailServiceTest {
    @Test
    void sendsHtmlInvoiceOnceAndTracksDelivery() {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        InvoiceEmailService service = new InvoiceEmailService(sender, true, "", "https://greenfarm.example", 5, 2);
        Payment payment = paymentWithOrder();

        service.sendInvoiceIfEnabled(payment);
        service.sendInvoiceIfEnabled(payment);

        verify(sender, times(1)).send(any(MimeMessage.class));
        assertThat(payment.getInvoiceEmailSentAt()).isNotNull();
    }

    @Test
    void leavesPaymentUntouchedWhenEmailIsDisabled() {
        JavaMailSender sender = mock(JavaMailSender.class);
        InvoiceEmailService service = new InvoiceEmailService(sender, false, "", "https://greenfarm.example", 5, 2);
        Payment payment = paymentWithOrder();

        service.sendInvoiceIfEnabled(payment);

        verifyNoInteractions(sender);
        assertThat(payment.getInvoiceEmailSentAt()).isNull();
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
        Payment payment = new Payment(); payment.setOrder(order); payment.setPaymentMethod(PaymentMethod.COD);
        return payment;
    }
}
