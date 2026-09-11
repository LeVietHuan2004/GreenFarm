package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleServiceTest {
    @Mock OrderRepository orders;
    @Mock OrderStatusHistoryRepository histories;
    @Mock PaymentRepository payments;
    @Mock ProductRepository products;
    @Mock CouponRepository coupons;
    OrderLifecycleService service;

    @BeforeEach void setUp() {
        service = new OrderLifecycleService(orders, histories, payments, products, coupons);
        lenient().when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(payments.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test void failedPaymentCancellationRestoresStockAndCouponOnlyOnce() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(5L); when(product.getStock()).thenReturn(3);
        when(product.getStatus()).thenReturn(ProductStatus.OUT_OF_STOCK);
        OrderItem item = new OrderItem(); item.setProduct(product); item.setQuantity(2); item.setPrice(BigDecimal.TEN);
        Coupon coupon = mock(Coupon.class);
        when(coupon.getId()).thenReturn(7L); when(coupon.getTimesUsed()).thenReturn(2);
        Order order = new Order(); order.setStatus(OrderStatus.PENDING); order.setCoupon(coupon); order.addItem(item);
        Payment payment = new Payment(); payment.setOrder(order); payment.setPaymentMethod(PaymentMethod.VNPAY); payment.setStatus(PaymentStatus.FAILED);

        when(orders.findByIdForUpdate(11L)).thenReturn(Optional.of(order));
        when(payments.findByOrder_Id(11L)).thenReturn(Optional.of(payment));
        when(products.findAllByIdForUpdate(List.of(5L))).thenReturn(List.of(product));
        when(coupons.findByIdForUpdate(7L)).thenReturn(Optional.of(coupon));

        service.cancelForFailedPayment(11L, "Thanh toán thất bại");
        service.cancelForFailedPayment(11L, "Lặp callback");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getInventoryReleasedAt()).isNotNull();
        verify(product).setStock(5); verify(product).setStatus(ProductStatus.IN_STOCK);
        verify(coupon).setTimesUsed(1);
        verify(products, times(1)).findAllByIdForUpdate(List.of(5L));
        verify(histories, times(1)).save(any(OrderStatusHistory.class));
    }

    @Test void deliveredCodOrderCompletesPayment() {
        Order order = new Order(); order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        Payment payment = new Payment(); payment.setOrder(order); payment.setPaymentMethod(PaymentMethod.COD); payment.setStatus(PaymentStatus.PENDING);
        when(orders.findByIdForUpdate(12L)).thenReturn(Optional.of(order));
        when(payments.findByOrder_Id(12L)).thenReturn(Optional.of(payment));

        service.updateStatus(12L, "delivered", null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getPaidAt()).isNotNull();
        assertThat(payment.getGatewayResponseCode()).isEqualTo("COD_COLLECTED");
    }

    @Test void onlineOrderCannotProcessBeforePaymentCompletes() {
        Order order = new Order(); order.setStatus(OrderStatus.PENDING);
        Payment payment = new Payment(); payment.setOrder(order); payment.setPaymentMethod(PaymentMethod.VNPAY); payment.setStatus(PaymentStatus.PENDING);
        when(orders.findByIdForUpdate(13L)).thenReturn(Optional.of(order));
        when(payments.findByOrder_Id(13L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.updateStatus(13L, "processing", null))
            .extracting("code").isEqualTo("PAYMENT_NOT_COMPLETED");
        verify(histories, never()).save(any());
    }
}
