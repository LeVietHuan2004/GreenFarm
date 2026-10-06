package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.dto.request.CheckoutRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orders;
    @Mock OrderStatusHistoryRepository histories;
    @Mock ShippingAddressService addresses;
    @Mock CartItemRepository carts;
    @Mock ProductRepository products;
    @Mock CouponEngineService couponEngine;
    @Mock UserRepository users;
    @Mock PaymentService paymentService;
    @Mock OrderLifecycleService orderLifecycle;
    @Mock NotificationService notifications;
    @Mock LoyaltyService loyalty;
    @Mock VnpayRefundService vnpayRefunds;
    @Mock InventoryService inventory;
    OrderService service;
    Product product;
    CartItem cartItem;
    ShippingAddress address;

    @BeforeEach void setUp(){
        service=new OrderService(orders,histories,addresses,carts,products,couponEngine,users,paymentService,orderLifecycle,notifications,loyalty,vnpayRefunds,inventory);
        product=mock(Product.class);
        lenient().when(product.getId()).thenReturn(10L);
        lenient().when(product.getName()).thenReturn("Rau sạch");
        lenient().when(product.getSlug()).thenReturn("rau-sach");
        lenient().when(product.getPrice()).thenReturn(new BigDecimal("200000"));
        lenient().when(product.getStock()).thenReturn(5);
        lenient().when(product.getStatus()).thenReturn(ProductStatus.IN_STOCK);
        lenient().when(product.getImages()).thenReturn(List.of());
        cartItem=new CartItem();cartItem.setProduct(product);cartItem.setQuantity(2);
        address=new ShippingAddress();address.setFullName("Nguyễn Văn A");address.setPhone("0901234567");address.setAddress("1 Đường X");address.setCity("TP.HCM");
        lenient().when(addresses.findEntity(1L,2L)).thenReturn(address);
        lenient().when(carts.findAllByUser_IdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(cartItem));
        User user = new User(); user.setLoyaltyPointsBalance(100);
        lenient().when(users.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(users.findByIdForCommerceUpdate(1L)).thenReturn(Optional.of(user));
        lenient().when(loyalty.quote(any(), any(), any())).thenReturn(new LoyaltyService.Quote(0, BigDecimal.ZERO));
        lenient().when(couponEngine.preview(anyLong(), anyList(), any(), any(), any(), any())).thenReturn(emptyCouponQuote());
        lenient().when(couponEngine.quoteForReservation(anyLong(), anyList(), any(), any(), any(), any())).thenReturn(emptyCouponQuote());
    }

    @Test void previewCalculatesStandardAndFreeShipping(){
        var standard=service.preview(1L,new CheckoutRequest(2L,null,null,null,null));
        assertThat(standard.subtotal()).isEqualByComparingTo("400000");
        assertThat(standard.shippingFee()).isEqualByComparingTo("30000");
        assertThat(standard.total()).isEqualByComparingTo("430000");
        cartItem.setQuantity(3);
        var free=service.preview(1L,new CheckoutRequest(2L,null,null,null,null));
        assertThat(free.shippingFee()).isZero();
        assertThat(free.total()).isEqualByComparingTo("600000");
    }

    @Test void adminOrderViewsUsePagedStatusGroups(){
        var pageable = PageRequest.of(2, 20);
        var active = List.of(OrderStatus.PENDING, OrderStatus.PROCESSING, OrderStatus.READY_FOR_DELIVERY,
            OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.DELIVERY_FAILED);
        var history = List.of(OrderStatus.COMPLETED, OrderStatus.CANCELED);
        when(orders.findAllByStatusInOrderByCreatedAtDescIdDesc(active, pageable)).thenReturn(Page.empty(pageable));
        when(orders.findAllByStatusInOrderByCreatedAtDescIdDesc(history, pageable)).thenReturn(Page.empty(pageable));

        assertThat(service.findAdminOrders(null, "active", pageable).page()).isEqualTo(2);
        assertThat(service.findAdminOrders(null, "history", pageable).page()).isEqualTo(2);
        verify(orders).findAllByStatusInOrderByCreatedAtDescIdDesc(active, pageable);
        verify(orders).findAllByStatusInOrderByCreatedAtDescIdDesc(history, pageable);
    }

    @Test void adminOrderStatusCannotEscapeSelectedView(){
        var pageable = PageRequest.of(0, 20);
        assertThat(service.findAdminOrders("canceled", "active", pageable).totalElements()).isZero();
        verifyNoInteractions(orders);
    }

    @Test void previewAppliesPercentageCouponAndCapsDiscount(){
        Coupon coupon=mock(Coupon.class); when(coupon.getCode()).thenReturn("SAVE20");
        when(couponEngine.preview(eq(1L), anyList(), eq(new BigDecimal("400000.00")), eq(new BigDecimal("30000.00")), eq("SAVE20"), isNull()))
            .thenReturn(new CouponEngineService.Quote(coupon, null, new BigDecimal("80000.00"), BigDecimal.ZERO, "Giảm 20%", null));
        var result=service.preview(1L,new CheckoutRequest(2L,"SAVE20",null,null,null));
        assertThat(result.discountAmount()).isEqualByComparingTo("80000");
        assertThat(result.total()).isEqualByComparingTo("350000");
    }

    @Test void createOrderSnapshotsValuesDecrementsStockAndClearsCart(){
        when(products.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(product));
        when(orders.save(any(Order.class))).thenAnswer(call->call.getArgument(0));
        var result=service.create(1L,new CheckoutRequest(2L,null,null,"cod",null),"127.0.0.1");
        assertThat(result.status()).isEqualTo("pending");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().productName()).isEqualTo("Rau sạch");
        assertThat(result.statusHistory()).singleElement().extracting("note").isEqualTo("Đơn hàng đã được tạo");
        verify(inventory).reserveOrder(any(Order.class), anyMap());
        verify(paymentService).createForOrder(any(Order.class), eq(PaymentMethod.COD), eq("127.0.0.1"));
        verify(carts).deleteAllByUser_Id(1L);
        verify(orders).save(any(Order.class));
    }

    @Test void vnpayReceivesServerCalculatedTotalAfterCouponPointsAndShipping() {
        Coupon coupon = mock(Coupon.class);
        Coupon shippingCoupon = mock(Coupon.class);
        when(coupon.getCode()).thenReturn("SAVE20");
        when(shippingCoupon.getCode()).thenReturn("SHIP15");
        when(products.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(product));
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        when(couponEngine.quoteForReservation(eq(1L), anyList(), eq(new BigDecimal("400000.00")), eq(new BigDecimal("30000.00")), eq("SAVE20"), eq("SHIP15")))
            .thenReturn(new CouponEngineService.Quote(coupon, shippingCoupon, new BigDecimal("80000.00"), new BigDecimal("15000.00"), "Giảm 20%", "Giảm phí vận chuyển 15000đ"));
        when(loyalty.quote(any(), eq(100), eq(new BigDecimal("320000.00"))))
            .thenReturn(new LoyaltyService.Quote(100, new BigDecimal("10000.00")));
        var capturedOrder = org.mockito.ArgumentCaptor.forClass(Order.class);

        service.create(1L, new CheckoutRequest(2L, "SAVE20", "SHIP15", "vnpay", 100), "127.0.0.1");

        verify(paymentService).createForOrder(capturedOrder.capture(), eq(PaymentMethod.VNPAY), eq("127.0.0.1"));
        assertThat(capturedOrder.getValue().getSubtotal()).isEqualByComparingTo("400000.00");
        assertThat(capturedOrder.getValue().getDiscountAmount()).isEqualByComparingTo("80000.00");
        assertThat(capturedOrder.getValue().getShippingFee()).isEqualByComparingTo("30000.00");
        assertThat(capturedOrder.getValue().getLoyaltyDiscountAmount()).isEqualByComparingTo("10000.00");
        assertThat(capturedOrder.getValue().getShippingDiscountAmount()).isEqualByComparingTo("15000.00");
        assertThat(capturedOrder.getValue().getTotalPrice()).isEqualByComparingTo("325000.00");
        verify(couponEngine).reserve(any(User.class), same(capturedOrder.getValue()), any(CouponEngineService.Quote.class));
    }

    @Test void stockFailureDoesNotCreateOrderOrClearCart(){
        when(products.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(product));
        when(product.getStock()).thenReturn(1);
        assertThatThrownBy(()->service.create(1L,new CheckoutRequest(2L,null,null,"cod",null),"127.0.0.1"))
            .extracting("code").isEqualTo("INSUFFICIENT_STOCK");
        verify(orders,never()).save(any());
        verify(carts,never()).deleteAllByUser_Id(any());
    }

    @Test void expiredCouponIsRejected(){
        when(couponEngine.preview(eq(1L), anyList(), any(), any(), eq("OLD"), isNull()))
            .thenThrow(new com.agri.ecommerce.common.exception.ApplicationException(org.springframework.http.HttpStatus.CONFLICT, "COUPON_EXPIRED", "Mã đã hết hạn"));
        assertThatThrownBy(()->service.preview(1L,new CheckoutRequest(2L,"OLD",null,null,null)))
            .extracting("code").isEqualTo("COUPON_EXPIRED");
    }

    private CouponEngineService.Quote emptyCouponQuote() {
        return new CouponEngineService.Quote(null, null, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }
}
