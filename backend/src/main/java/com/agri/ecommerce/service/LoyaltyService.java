package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.response.LoyaltyPointTransactionResponse;
import com.agri.ecommerce.dto.response.LoyaltySummaryResponse;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.LoyaltyPointTransactionRepository;
import com.agri.ecommerce.repository.OrderRepository;
import com.agri.ecommerce.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoyaltyService {
    public static final int EARN_AMOUNT_PER_POINT = 10_000;
    public static final BigDecimal DISCOUNT_PER_POINT = new BigDecimal("100");
    public static final int MAX_REDEMPTION_PERCENT = 50;
    private static final String ORDER_EARN = "order_earn";
    private static final String REVIEW_EARN = "review_earn";
    private static final String REDEMPTION = "redemption";
    private static final String REDEMPTION_RESTORE = "redemption_restore";
    private static final List<OrderStatus> PURCHASED_STATUSES = List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    private final LoyaltyPointTransactionRepository transactions;
    private final UserRepository users;
    private final OrderRepository orders;

    public LoyaltyService(LoyaltyPointTransactionRepository transactions, UserRepository users, OrderRepository orders) {
        this.transactions = transactions; this.users = users; this.orders = orders;
    }

    @Transactional(readOnly = true)
    public LoyaltySummaryResponse summary(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> notFound());
        return new LoyaltySummaryResponse(user.getLoyaltyPointsBalance(), DISCOUNT_PER_POINT, EARN_AMOUNT_PER_POINT,
            MAX_REDEMPTION_PERCENT, transactions.findTop30ByUser_IdOrderByCreatedAtDescIdDesc(userId).stream().map(this::toResponse).toList());
    }

    public Quote quote(User user, Integer requestedPoints, BigDecimal amountBeforeLoyaltyDiscount) {
        int requested = requestedPoints == null ? 0 : requestedPoints;
        if (requested > user.getLoyaltyPointsBalance()) {
            throw conflict("LOYALTY_INSUFFICIENT_POINTS", "Bạn không có đủ điểm để sử dụng");
        }
        int maxPoints = amountBeforeLoyaltyDiscount.multiply(BigDecimal.valueOf(MAX_REDEMPTION_PERCENT))
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN)
            .divide(DISCOUNT_PER_POINT, 0, RoundingMode.DOWN).intValue();
        int applied = Math.min(requested, maxPoints);
        return new Quote(applied, DISCOUNT_PER_POINT.multiply(BigDecimal.valueOf(applied)).setScale(2));
    }

    @Transactional
    public void redeem(User currentUser, Order order, int points) {
        if (points <= 0 || transactions.existsByTypeAndOrder_Id(REDEMPTION, order.getId())) return;
        User user = users.findByIdForCommerceUpdate(currentUser.getId()).orElseThrow(() -> notFound());
        if (user.getLoyaltyPointsBalance() < points) throw conflict("LOYALTY_INSUFFICIENT_POINTS", "Bạn không có đủ điểm để sử dụng");
        user.setLoyaltyPointsBalance(user.getLoyaltyPointsBalance() - points);
        users.save(user);
        record(user, order, null, null, REDEMPTION, -points, "Dùng điểm cho đơn hàng #" + order.getId());
    }

    @Transactional
    public void restoreRedemption(Order order) {
        if (!transactions.existsByTypeAndOrder_Id(REDEMPTION, order.getId()) || transactions.existsByTypeAndOrder_Id(REDEMPTION_RESTORE, order.getId())) return;
        LoyaltyPointTransaction redemption = transactions.findByTypeAndOrder_Id(REDEMPTION, order.getId()).orElse(null);
        if (redemption == null) return;
        User user = users.findByIdForCommerceUpdate(order.getUser().getId()).orElseThrow(() -> notFound());
        int restored = Math.abs(redemption.getPoints());
        user.setLoyaltyPointsBalance(user.getLoyaltyPointsBalance() + restored);
        users.save(user);
        record(user, order, null, null, REDEMPTION_RESTORE, restored, "Hoàn điểm do hủy đơn hàng #" + order.getId());
    }

    @Transactional
    public void rewardOrder(Order order) {
        if (transactions.existsByTypeAndOrder_Id(ORDER_EARN, order.getId())) return;
        int points = pointsForAmount(order.getTotalPrice());
        if (points == 0) return;
        User user = users.findByIdForCommerceUpdate(order.getUser().getId()).orElseThrow(() -> notFound());
        user.setLoyaltyPointsBalance(user.getLoyaltyPointsBalance() + points);
        users.save(user);
        record(user, order, null, null, ORDER_EARN, points, "Điểm thưởng từ đơn hàng #" + order.getId());
    }

    @Transactional
    public void rewardReview(Long userId, Review review) {
        if (review.getId() == null || review.getProduct() == null || review.getProduct().getId() == null) return;
        Long productId = review.getProduct().getId();
        if (transactions.existsByTypeAndUser_IdAndProduct_Id(REVIEW_EARN, userId, productId)) return;
        OrderItem purchasedItem = orders.findPurchasedItems(userId, productId, PURCHASED_STATUSES, PageRequest.of(0, 1))
            .stream().findFirst().orElse(null);
        if (purchasedItem == null) return;
        int points = pointsForAmount(purchasedItem.getPrice().multiply(BigDecimal.valueOf(purchasedItem.getQuantity())));
        if (points == 0) return;
        User user = users.findByIdForCommerceUpdate(userId).orElseThrow(() -> notFound());
        if (transactions.existsByTypeAndUser_IdAndProduct_Id(REVIEW_EARN, userId, productId)) return;
        user.setLoyaltyPointsBalance(user.getLoyaltyPointsBalance() + points);
        users.save(user);
        record(user, null, review, review.getProduct(), REVIEW_EARN, points, "Điểm thưởng đánh giá " + review.getProduct().getName());
    }

    private int pointsForAmount(BigDecimal amount) { return amount.divide(BigDecimal.valueOf(EARN_AMOUNT_PER_POINT), 0, RoundingMode.DOWN).intValue(); }
    private void record(User user, Order order, Review review, Product product, String type, int points, String description) {
        LoyaltyPointTransaction transaction = new LoyaltyPointTransaction();
        transaction.setUser(user); transaction.setOrder(order); transaction.setReview(review); transaction.setProduct(product); transaction.setType(type);
        transaction.setPoints(points); transaction.setDescription(description); transactions.save(transaction);
    }
    private LoyaltyPointTransactionResponse toResponse(LoyaltyPointTransaction value) { return new LoyaltyPointTransactionResponse(value.getId(), value.getType(), value.getPoints(), value.getDescription(), value.getCreatedAt()); }
    private ApplicationException notFound() { return new ApplicationException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy tài khoản"); }
    private ApplicationException conflict(String code, String message) { return new ApplicationException(HttpStatus.CONFLICT, code, message); }
    public record Quote(int pointsApplied, BigDecimal discountAmount) {}
}
