package com.agri.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.agri.ecommerce.entity.Order;
import com.agri.ecommerce.entity.OrderItem;
import com.agri.ecommerce.entity.Product;
import com.agri.ecommerce.entity.Review;
import com.agri.ecommerce.entity.User;
import com.agri.ecommerce.repository.LoyaltyPointTransactionRepository;
import com.agri.ecommerce.repository.OrderRepository;
import com.agri.ecommerce.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LoyaltyServiceTest {
    private final LoyaltyPointTransactionRepository transactions = mock(LoyaltyPointTransactionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final LoyaltyService service = new LoyaltyService(transactions, users, orders);

    @Test
    void capsPointRedemptionAtHalfTheOrderValue() {
        User user = new User(); user.setLoyaltyPointsBalance(500);
        LoyaltyService.Quote quote = service.quote(user, 500, new BigDecimal("20000"));
        assertThat(quote.pointsApplied()).isEqualTo(100);
        assertThat(quote.discountAmount()).isEqualByComparingTo("10000");
    }

    @Test
    void rejectsUsingMorePointsThanBalance() {
        User user = new User(); user.setLoyaltyPointsBalance(9);
        assertThatThrownBy(() -> service.quote(user, 10, new BigDecimal("100000")))
            .extracting("code").isEqualTo("LOYALTY_INSUFFICIENT_POINTS");
    }

    @Test
    void rewardsACompletedOrderOnlyOnce() {
        User orderUser = mock(User.class); when(orderUser.getId()).thenReturn(1L);
        User lockedUser = mock(User.class); when(lockedUser.getLoyaltyPointsBalance()).thenReturn(4);
        Order order = mock(Order.class); when(order.getId()).thenReturn(8L); when(order.getUser()).thenReturn(orderUser);
        when(order.getTotalPrice()).thenReturn(new BigDecimal("50000"));
        when(transactions.existsByTypeAndOrder_Id("order_earn", 8L)).thenReturn(false, true);
        when(users.findByIdForCommerceUpdate(1L)).thenReturn(Optional.of(lockedUser));

        service.rewardOrder(order);

        verify(lockedUser).setLoyaltyPointsBalance(9);
        verify(transactions).save(any());
        service.rewardOrder(order);
        verify(transactions, times(1)).save(any());
    }

    @Test
    void rewardsReviewOnlyOnceForAUserAndProductAfterReviewIsRecreated() {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(20L);
        when(product.getName()).thenReturn("Rau sạch");
        Review originalReview = mock(Review.class);
        when(originalReview.getId()).thenReturn(30L);
        when(originalReview.getProduct()).thenReturn(product);
        Review recreatedReview = mock(Review.class);
        when(recreatedReview.getId()).thenReturn(31L);
        when(recreatedReview.getProduct()).thenReturn(product);
        OrderItem purchasedItem = new OrderItem();
        purchasedItem.setPrice(new BigDecimal("30000"));
        purchasedItem.setQuantity(2);
        User lockedUser = new User();
        lockedUser.setLoyaltyPointsBalance(4);

        when(transactions.existsByTypeAndUser_IdAndProduct_Id("review_earn", 1L, 20L))
            .thenReturn(false, false, true);
        when(orders.findPurchasedItems(eq(1L), eq(20L), anyList(), any())).thenReturn(List.of(purchasedItem));
        when(users.findByIdForCommerceUpdate(1L)).thenReturn(Optional.of(lockedUser));

        service.rewardReview(1L, originalReview);
        service.rewardReview(1L, recreatedReview);

        assertThat(lockedUser.getLoyaltyPointsBalance()).isEqualTo(10);
        verify(transactions, times(1)).save(argThat(transaction ->
            transaction.getReview() == originalReview && transaction.getProduct() == product));
    }
}
