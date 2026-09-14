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

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orders;
    @Mock OrderStatusHistoryRepository histories;
    @Mock ShippingAddressService addresses;
    @Mock CartItemRepository carts;
    @Mock ProductRepository products;
    @Mock CouponRepository coupons;
    @Mock UserRepository users;
    @Mock PaymentService paymentService;
    @Mock OrderLifecycleService orderLifecycle;
    @Mock NotificationService notifications;
    OrderService service;
    Product product;
    CartItem cartItem;
    ShippingAddress address;

    @BeforeEach void setUp(){
        service=new OrderService(orders,histories,addresses,carts,products,coupons,users,paymentService,orderLifecycle,notifications);
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
    }

    @Test void previewCalculatesStandardAndFreeShipping(){
        var standard=service.preview(1L,new CheckoutRequest(2L,null,null));
        assertThat(standard.subtotal()).isEqualByComparingTo("400000");
        assertThat(standard.shippingFee()).isEqualByComparingTo("30000");
        assertThat(standard.total()).isEqualByComparingTo("430000");
        cartItem.setQuantity(3);
        var free=service.preview(1L,new CheckoutRequest(2L,null,null));
        assertThat(free.shippingFee()).isZero();
        assertThat(free.total()).isEqualByComparingTo("600000");
    }

    @Test void previewAppliesPercentageCouponAndCapsDiscount(){
        Coupon coupon=mock(Coupon.class);
        when(coupons.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(coupon));
        when(coupon.isActive()).thenReturn(true);when(coupon.getCode()).thenReturn("SAVE20");
        when(coupon.getUsageLimit()).thenReturn(null);
        when(coupon.getCouponType()).thenReturn(CouponType.ORDER_DISCOUNT);when(coupon.getDiscountType()).thenReturn(DiscountType.PERCENTAGE);
        when(coupon.getDiscountPercentage()).thenReturn(20);
        var result=service.preview(1L,new CheckoutRequest(2L,"SAVE20",null));
        assertThat(result.discountAmount()).isEqualByComparingTo("80000");
        assertThat(result.total()).isEqualByComparingTo("350000");
    }

    @Test void createOrderSnapshotsValuesDecrementsStockAndClearsCart(){
        when(users.findByIdForCommerceUpdate(1L)).thenReturn(Optional.of(new User()));
        when(products.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(product));
        when(orders.save(any(Order.class))).thenAnswer(call->call.getArgument(0));
        var result=service.create(1L,new CheckoutRequest(2L,null,"cod"),"127.0.0.1");
        assertThat(result.status()).isEqualTo("pending");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().productName()).isEqualTo("Rau sạch");
        assertThat(result.statusHistory()).singleElement().extracting("note").isEqualTo("Đơn hàng đã được tạo");
        verify(product).setStock(3);
        verify(paymentService).createForOrder(any(Order.class), eq(PaymentMethod.COD), eq("127.0.0.1"));
        verify(carts).deleteAllByUser_Id(1L);
        verify(orders).save(any(Order.class));
    }

    @Test void stockFailureDoesNotCreateOrderOrClearCart(){
        when(users.findByIdForCommerceUpdate(1L)).thenReturn(Optional.of(new User()));
        when(products.findAllByIdForUpdate(List.of(10L))).thenReturn(List.of(product));
        when(product.getStock()).thenReturn(1);
        assertThatThrownBy(()->service.create(1L,new CheckoutRequest(2L,null,"cod"),"127.0.0.1"))
            .extracting("code").isEqualTo("INSUFFICIENT_STOCK");
        verify(orders,never()).save(any());
        verify(carts,never()).deleteAllByUser_Id(any());
    }

    @Test void expiredCouponIsRejected(){
        Coupon coupon=mock(Coupon.class);
        when(coupons.findByCodeIgnoreCase("OLD")).thenReturn(Optional.of(coupon));
        when(coupon.isActive()).thenReturn(false);
        assertThatThrownBy(()->service.preview(1L,new CheckoutRequest(2L,"OLD",null)))
            .extracting("code").isEqualTo("INVALID_COUPON");
    }
}
