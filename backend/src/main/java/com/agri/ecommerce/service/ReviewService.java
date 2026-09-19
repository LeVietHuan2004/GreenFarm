package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.dto.request.ReviewRequest;
import com.agri.ecommerce.dto.response.*;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.*;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ReviewService {
    private static final List<OrderStatus> PURCHASED_STATUSES = List.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED);
    private final ReviewRepository reviews;
    private final ProductRepository products;
    private final UserRepository users;
    private final OrderRepository orders;
    private final LoyaltyService loyalty;

    public ReviewService(ReviewRepository reviews, ProductRepository products, UserRepository users, OrderRepository orders, LoyaltyService loyalty) {
        this.reviews = reviews; this.products = products; this.users = users; this.orders = orders; this.loyalty = loyalty;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> findByProduct(Long productId, Pageable pageable) {
        if (!products.existsById(productId)) throw notFound("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm");
        return PageResponse.from(reviews.findAllByProduct_IdOrderByCreatedAtDescIdDesc(productId, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReviewEligibilityResponse eligibility(Long userId, Long productId) {
        boolean purchased = orders.hasPurchasedProduct(userId, productId, PURCHASED_STATUSES);
        ReviewResponse existing = reviews.findByUser_IdAndProduct_Id(userId, productId).map(this::toResponse).orElse(null);
        return new ReviewEligibilityResponse(purchased, purchased && existing == null, existing);
    }

    @Transactional
    public ReviewResponse create(Long userId, ReviewRequest request) {
        if (!orders.hasPurchasedProduct(userId, request.productId(), PURCHASED_STATUSES)) {
            throw new ApplicationException(HttpStatus.FORBIDDEN, "REVIEW_PURCHASE_REQUIRED", "Bạn chỉ có thể đánh giá sản phẩm đã nhận hàng");
        }
        if (reviews.findByUser_IdAndProduct_Id(userId, request.productId()).isPresent()) {
            throw new ApplicationException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "Bạn đã đánh giá sản phẩm này");
        }
        Review review = new Review();
        review.setUser(users.getReferenceById(userId));
        review.setProduct(products.findById(request.productId()).orElseThrow(() -> notFound("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm")));
        apply(review, request);
        Review saved = reviews.save(review);
        loyalty.rewardReview(userId, saved);
        return toResponse(saved);
    }

    @Transactional
    public ReviewResponse update(Long userId, Long id, ReviewRequest request) {
        Review review = reviews.findByIdAndUser_Id(id, userId).orElseThrow(() -> notFound("REVIEW_NOT_FOUND", "Không tìm thấy đánh giá"));
        if (!review.getProduct().getId().equals(request.productId())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "REVIEW_PRODUCT_MISMATCH", "Không thể đổi sản phẩm của đánh giá");
        }
        apply(review, request);
        return toResponse(reviews.save(review));
    }

    private void apply(Review review, ReviewRequest request) {
        review.setRating(request.rating());
        review.setComment(StringUtils.hasText(request.comment()) ? request.comment().trim() : null);
    }
    private ReviewResponse toResponse(Review value) { return new ReviewResponse(value.getId(), value.getProduct().getId(), value.getUser().getId(), value.getUser().getName(), value.getRating(), value.getComment(), value.getCreatedAt(), value.getUpdatedAt()); }
    private ApplicationException notFound(String code, String message) { return new ApplicationException(HttpStatus.NOT_FOUND, code, message); }
}
