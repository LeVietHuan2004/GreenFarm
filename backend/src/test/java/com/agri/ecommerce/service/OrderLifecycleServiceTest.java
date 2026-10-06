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
    @Mock CouponEngineService couponEngine;
    @Mock UserRepository users;
    @Mock NotificationService notifications;
    @Mock LoyaltyService loyalty;
    @Mock InventoryService inventory;
    OrderLifecycleService service;

    @BeforeEach void setUp() {
        service = new OrderLifecycleService(orders, histories, payments, products, couponEngine, users, notifications, loyalty, inventory);
        lenient().when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(payments.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test void failedPaymentCancellationRestoresStockAndCouponOnlyOnce() {
        Product product = mock(Product.class);
        OrderItem item = new OrderItem(); item.setProduct(product); item.setQuantity(2); item.setPrice(BigDecimal.TEN);
        Order order = new Order(); order.setStatus(OrderStatus.PENDING); order.addItem(item);
        Payment payment = new Payment(); payment.setOrder(order); payment.setPaymentMethod(PaymentMethod.VNPAY); payment.setStatus(PaymentStatus.FAILED);

        when(orders.findByIdForUpdate(11L)).thenReturn(Optional.of(order));
        when(payments.findByOrder_Id(11L)).thenReturn(Optional.of(payment));

        service.cancelForFailedPayment(11L, "Thanh toán thất bại");
        service.cancelForFailedPayment(11L, "Lặp callback");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getInventoryReleasedAt()).isNotNull();
        verify(inventory).restoreOrder(order);
        verify(couponEngine, times(2)).releaseForCancellation(order, false);
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
        verify(loyalty).rewardOrder(order);
    }

    @Test void confirmingARefundCancelsAndRestoresRedeemedPointsOnlyOnce() {
        Order order = new Order();
        order.setStatus(OrderStatus.PROCESSING);
        order.setLoyaltyPointsUsed(120);
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.COMPLETED);
        when(orders.findByIdForUpdate(20L)).thenReturn(Optional.of(order));
        when(payments.findByOrder_Id(20L)).thenReturn(Optional.of(payment));

        service.confirmRefundAndCancel(20L, "Đã hoàn tiền qua VNPAY");
        service.confirmRefundAndCancel(20L, "Callback lặp");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getGatewayResponseCode()).isEqualTo("REFUND_CONFIRMED");
        verify(loyalty, times(1)).restoreRedemption(order);
        verify(histories, times(1)).save(any(OrderStatusHistory.class));
        verify(payments, times(1)).save(payment);
    }

    @Test void completedRefundKeepsCouponUsageForAudit() {
        Order order = new Order(); order.setStatus(OrderStatus.COMPLETED);
        Payment payment = new Payment(); payment.setOrder(order); payment.setPaymentMethod(PaymentMethod.VNPAY); payment.setStatus(PaymentStatus.COMPLETED);
        when(orders.findByIdForUpdate(21L)).thenReturn(Optional.of(order));
        when(payments.findByOrder_Id(21L)).thenReturn(Optional.of(payment));

        service.confirmRefundAndCancel(21L, "Hoàn đơn đã hoàn tất");

        verify(couponEngine).releaseForCancellation(order, true);
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

    @Test void deliveryStaffCanClaimAndStartAssignedOrder() {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn("delivery_staff");
        User deliveryStaff = mock(User.class);
        when(deliveryStaff.getId()).thenReturn(44L); when(deliveryStaff.getRole()).thenReturn(role); when(deliveryStaff.getStatus()).thenReturn(UserStatus.ACTIVE);
        Order order = new Order(); order.setStatus(OrderStatus.READY_FOR_DELIVERY); order.setDeliveryStaff(deliveryStaff);
        when(orders.findByIdForUpdate(14L)).thenReturn(Optional.of(order));
        when(users.findById(44L)).thenReturn(Optional.of(deliveryStaff));
        when(payments.findByOrder_Id(14L)).thenReturn(Optional.empty());

        service.claimForDelivery(14L, 44L);
        service.updateByDelivery(14L, 44L, "out_for_delivery", null);

        assertThat(order.getDeliveryStaff()).isSameAs(deliveryStaff);
        assertThat(order.getDeliveryClaimedAt()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        assertThat(order.getDispatchedAt()).isNotNull();
        verify(histories, times(2)).save(any(OrderStatusHistory.class));
    }

    @Test void deliveryStaffCannotUpdateAnotherPersonsOrder() {
        User assignedStaff = mock(User.class);
        when(assignedStaff.getId()).thenReturn(44L);
        Order order = new Order(); order.setStatus(OrderStatus.READY_FOR_DELIVERY); order.setDeliveryStaff(assignedStaff);
        when(orders.findByIdForUpdate(15L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateByDelivery(15L, 45L, "out_for_delivery", null))
            .extracting("code").isEqualTo("ORDER_NOT_ASSIGNED_TO_DELIVERY");
    }

    @Test void deliveryStaffCannotClaimAnUnassignedOrder() {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn("delivery_staff");
        User deliveryStaff = mock(User.class);
        when(deliveryStaff.getRole()).thenReturn(role);
        when(deliveryStaff.getStatus()).thenReturn(UserStatus.ACTIVE);
        Order order = new Order();
        order.setStatus(OrderStatus.READY_FOR_DELIVERY);
        when(orders.findByIdForUpdate(19L)).thenReturn(Optional.of(order));
        when(users.findById(44L)).thenReturn(Optional.of(deliveryStaff));

        assertThatThrownBy(() -> service.claimForDelivery(19L, 44L))
            .extracting("code").isEqualTo("ORDER_NOT_ASSIGNED_TO_DELIVERY");
        verify(histories, never()).save(any());
    }

    @Test void assignedDeliveryMustClaimBeforeStarting() {
        User deliveryStaff = mock(User.class);
        when(deliveryStaff.getId()).thenReturn(44L);
        Order order = new Order();
        order.setStatus(OrderStatus.READY_FOR_DELIVERY);
        order.setDeliveryStaff(deliveryStaff);
        when(orders.findByIdForUpdate(16L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateByDelivery(16L, 44L, "out_for_delivery", null))
            .extracting("code").isEqualTo("DELIVERY_ORDER_NOT_CLAIMED");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_DELIVERY);
    }

    @Test void claimingAnOrderTwiceIsIdempotent() {
        Role role = mock(Role.class);
        when(role.getName()).thenReturn("delivery_staff");
        User deliveryStaff = mock(User.class);
        when(deliveryStaff.getId()).thenReturn(44L);
        when(deliveryStaff.getName()).thenReturn("Nhân viên giao hàng");
        when(deliveryStaff.getRole()).thenReturn(role);
        when(deliveryStaff.getStatus()).thenReturn(UserStatus.ACTIVE);
        Order order = new Order();
        order.setStatus(OrderStatus.READY_FOR_DELIVERY);
        order.setDeliveryStaff(deliveryStaff);
        when(orders.findByIdForUpdate(17L)).thenReturn(Optional.of(order));
        when(users.findById(44L)).thenReturn(Optional.of(deliveryStaff));

        service.claimForDelivery(17L, 44L);
        var claimedAt = order.getDeliveryClaimedAt();
        service.claimForDelivery(17L, 44L);

        assertThat(claimedAt).isNotNull();
        assertThat(order.getDeliveryClaimedAt()).isEqualTo(claimedAt);
        verify(histories, times(1)).save(any(OrderStatusHistory.class));
    }

    @Test void failedDeliveryCanReturnToQueueAndClearsAssignment() {
        User deliveryStaff = mock(User.class);
        when(deliveryStaff.getId()).thenReturn(44L);
        Order order = new Order();
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setDeliveryStaff(deliveryStaff);
        order.setDeliveryClaimedAt(java.time.LocalDateTime.now());
        when(orders.findByIdForUpdate(18L)).thenReturn(Optional.of(order));
        when(payments.findByOrder_Id(18L)).thenReturn(Optional.empty());

        service.updateByDelivery(18L, 44L, "delivery_failed", "Khách không nghe máy");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERY_FAILED);
        assertThat(order.getDeliveryFailureReason()).isEqualTo("Khách không nghe máy");

        service.updateByStaff(18L, "ready_for_delivery", "Chuẩn bị giao lại");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY_FOR_DELIVERY);
        assertThat(order.getDeliveryStaff()).isNull();
        assertThat(order.getDeliveryClaimedAt()).isNull();
        assertThat(order.getDeliveryFailureReason()).isNull();
    }
}
