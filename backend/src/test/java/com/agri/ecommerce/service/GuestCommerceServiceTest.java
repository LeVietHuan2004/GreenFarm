package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.dto.request.CartItemRequest;
import com.agri.ecommerce.dto.request.GuestCheckoutRequest;
import com.agri.ecommerce.dto.request.GuestOrderLookupRequest;
import com.agri.ecommerce.dto.response.PaymentResponse;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GuestCommerceServiceTest {
    @Mock GuestSessionRepository sessions; @Mock GuestCartItemRepository guestCarts; @Mock CartItemRepository carts;
    @Mock ProductRepository products; @Mock UserRepository users; @Mock OrderRepository orders;
    @Mock CouponEngineService coupons; @Mock PaymentService payments; @Mock NotificationService notifications; @Mock OrderService orderService;
    @Mock InventoryService inventory;
    GuestCommerceService service; GuestSession session; Product product;

    @BeforeEach void setUp(){
        service=new GuestCommerceService(sessions,guestCarts,carts,products,users,orders,coupons,payments,notifications,orderService,inventory,30);
        session=mock(GuestSession.class); lenient().when(session.getId()).thenReturn(5L); lenient().when(session.getExpiresAt()).thenReturn(LocalDateTime.now().plusDays(1));
        lenient().when(sessions.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(session));
        lenient().when(sessions.findByTokenHash(anyString())).thenReturn(Optional.of(session));
        product=mock(Product.class);lenient().when(product.getId()).thenReturn(8L);lenient().when(product.getName()).thenReturn("Rau sạch");lenient().when(product.getSlug()).thenReturn("rau-sach");lenient().when(product.getStatus()).thenReturn(ProductStatus.IN_STOCK);lenient().when(product.getStock()).thenReturn(10);lenient().when(product.getPrice()).thenReturn(new BigDecimal("10000"));lenient().when(product.getCategory()).thenReturn(new Category());lenient().when(product.getImages()).thenReturn(List.of());
        lenient().when(products.findById(8L)).thenReturn(Optional.of(product));
        lenient().when(guestCarts.findAllByGuestSession_IdOrderByCreatedAtDescIdDesc(5L)).thenReturn(List.of());
    }

    @Test void repeatedAddUsesOneCartRowAndChecksCombinedStock(){
        GuestCartItem existing=new GuestCartItem();existing.setGuestSession(session);existing.setProduct(product);existing.setQuantity(3);
        when(guestCarts.findByGuestSession_IdAndProduct_Id(5L,8L)).thenReturn(Optional.of(existing));
        service.addItem("secret",new CartItemRequest(8L,2));
        assertThat(existing.getQuantity()).isEqualTo(5); verify(guestCarts).save(existing);
        when(product.getStock()).thenReturn(5);
        assertThatThrownBy(()->service.addItem("secret",new CartItemRequest(8L,1))).extracting("code").isEqualTo("INSUFFICIENT_STOCK");
    }

    @Test void mergeRevalidatesCombinedQuantityBeforeChangingEitherCart(){
        User user=new User(); when(users.findByIdForCommerceUpdate(1L)).thenReturn(Optional.of(user));
        GuestCartItem guest=new GuestCartItem();guest.setGuestSession(session);guest.setProduct(product);guest.setQuantity(3);
        when(guestCarts.findAllByGuestSession_IdOrderByCreatedAtDescIdDesc(5L)).thenReturn(List.of(guest));
        when(products.findAllByIdForUpdate(List.of(8L))).thenReturn(List.of(product));
        CartItem existing=new CartItem();existing.setProduct(product);existing.setQuantity(4);
        when(carts.findAllByUser_IdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(existing));
        when(product.getStock()).thenReturn(5);
        assertThatThrownBy(()->service.mergeIntoCustomer("secret",1L)).extracting("code").isEqualTo("INSUFFICIENT_STOCK");
        verify(carts,never()).save(any());verify(guestCarts,never()).deleteAllByGuestSession_Id(anyLong());
    }

    @Test void mergeRejectsUnavailableExistingItemEvenWhenGuestCartIsEmpty(){
        User user=new User();when(users.findByIdForCommerceUpdate(1L)).thenReturn(Optional.of(user));
        CartItem existing=new CartItem();existing.setProduct(product);existing.setQuantity(2);
        when(carts.findAllByUser_IdOrderByCreatedAtDescIdDesc(1L)).thenReturn(List.of(existing));
        when(products.findAllByIdForUpdate(List.of(8L))).thenReturn(List.of(product));
        when(product.getStatus()).thenReturn(ProductStatus.OUT_OF_STOCK);
        assertThatThrownBy(()->service.mergeIntoCustomer("secret",1L)).extracting("code").isEqualTo("PRODUCT_UNAVAILABLE");
        verify(sessions,never()).save(any());
    }

    @Test void repeatedCheckoutKeyReturnsExistingOrderWithoutChangingStock(){
        Order existing=mock(Order.class);when(existing.getId()).thenReturn(42L);
        when(orders.findByGuestSession_IdAndGuestCheckoutKey(5L,"repeat-key-123456")).thenReturn(Optional.of(existing));
        GuestCheckoutRequest request=new GuestCheckoutRequest("Nguyen Van A","0901234567","a@example.com","1 Duong X","Ha Noi","standard",null,null,"cod","repeat-key-123456");
        var result=service.checkout("secret",request,"127.0.0.1");
        assertThat(result.replayed()).isTrue();assertThat(result.lookupToken()).isNotBlank();
        verify(products,never()).findAllByIdForUpdate(any());verify(orders,never()).save(any());
    }

    @Test void repeatedVnpayCheckoutReturnsPendingGatewayUrl(){
        Order existing=mock(Order.class);when(existing.getId()).thenReturn(42L);
        when(orders.findByGuestSession_IdAndGuestCheckoutKey(5L,"repeat-key-123456")).thenReturn(Optional.of(existing));
        when(payments.pendingVnpayUrl(42L,"127.0.0.1")).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?request=42");
        GuestCheckoutRequest request=new GuestCheckoutRequest("Nguyen Van A","0901234567","a@example.com","1 Duong X","Ha Noi","standard",null,null,"vnpay","repeat-key-123456");
        var result=service.checkout("secret",request,"127.0.0.1");
        assertThat(result.replayed()).isTrue();
        assertThat(result.paymentUrl()).startsWith("https://sandbox.vnpayment.vn/");
        verify(payments,never()).createForOrder(any(),any(),any());
    }

    @Test void firstCheckoutReturnsGatewayUrlAndServerCalculatedAmount(){
        GuestCartItem guest=new GuestCartItem();guest.setGuestSession(session);guest.setProduct(product);guest.setQuantity(2);
        when(guestCarts.findAllByGuestSession_IdOrderByCreatedAtDescIdDesc(5L)).thenReturn(List.of(guest));
        when(products.findAllByIdForUpdate(List.of(8L))).thenReturn(List.of(product));
        when(coupons.quoteGuestForReservation(eq(5L),anyList(),any(),any(),isNull(),isNull()))
            .thenReturn(new CouponEngineService.Quote(null,null,BigDecimal.ZERO,BigDecimal.ZERO,null,null));
        when(orders.save(any(Order.class))).thenAnswer(call->{Order saved=call.getArgument(0);ReflectionTestUtils.setField(saved,"id",42L);return saved;});
        when(payments.createForOrder(any(Order.class),eq(PaymentMethod.VNPAY),eq("127.0.0.1")))
            .thenAnswer(call->{Order saved=call.getArgument(0);assertThat(saved.getTotalPrice()).isEqualByComparingTo("50000.00");
                return new PaymentResponse(9L,"vnpay","pending",saved.getTotalPrice(),"VNP-42",null,null,"https://sandbox.vnpayment.vn/pay?order=42");});
        GuestCheckoutRequest request=new GuestCheckoutRequest("Nguyen Van A","0901234567","a@example.com","1 Duong X","Ha Noi","standard",null,null,"vnpay","repeat-key-123456");
        var result=service.checkout("secret",request,"127.0.0.1");
        assertThat(result.replayed()).isFalse();
        assertThat(result.paymentUrl()).isEqualTo("https://sandbox.vnpayment.vn/pay?order=42");
        verify(inventory).reserveOrder(any(Order.class), anyMap());
        verify(guestCarts).deleteAllByGuestSession_Id(5L);
    }

    @Test void recoveryFindsCommittedOrderWithoutCreatingAnotherOne(){
        Order existing=mock(Order.class);when(existing.getId()).thenReturn(42L);
        when(orders.findByGuestSession_IdAndGuestCheckoutKey(5L,"repeat-key-123456")).thenReturn(Optional.of(existing));
        var result=service.recoverCheckout("secret","repeat-key-123456","127.0.0.1");
        assertThat(result.replayed()).isTrue();assertThat(result.lookupToken()).isNotBlank();
        verify(orders,never()).save(any());verify(products,never()).findAllByIdForUpdate(any());
    }

    @Test void recoveryRejectsUnknownOrMalformedKey(){
        assertThatThrownBy(()->service.recoverCheckout("secret","short","127.0.0.1"))
            .extracting("code").isEqualTo("ORDER_NOT_FOUND");
        assertThatThrownBy(()->service.recoverCheckout("secret","missing-key-123456","127.0.0.1"))
            .extracting("code").isEqualTo("ORDER_NOT_FOUND");
    }

    @Test void lookupRequiresCorrectEmailAndToken(){
        Order existing=new Order();existing.setGuestEmail("a@example.com");
        existing.setGuestLookupTokenHash("not-the-request-token-hash");
        when(orders.findByIdAndGuestEmailIgnoreCase(42L,"a@example.com")).thenReturn(Optional.of(existing));
        assertThatThrownBy(()->service.lookup(new GuestOrderLookupRequest(42L,"a@example.com","a".repeat(32))))
            .extracting("code").isEqualTo("ORDER_NOT_FOUND");
        assertThatThrownBy(()->service.lookup(new GuestOrderLookupRequest(42L,"wrong@example.com","a".repeat(32))))
            .extracting("code").isEqualTo("ORDER_NOT_FOUND");
        verifyNoInteractions(orderService);
    }
}
