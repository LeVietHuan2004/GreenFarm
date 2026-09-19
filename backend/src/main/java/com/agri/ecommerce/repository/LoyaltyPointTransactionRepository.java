package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.LoyaltyPointTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoyaltyPointTransactionRepository extends JpaRepository<LoyaltyPointTransaction, Long> {
    boolean existsByTypeAndOrder_Id(String type, Long orderId);
    boolean existsByTypeAndReview_Id(String type, Long reviewId);
    boolean existsByTypeAndUser_IdAndProduct_Id(String type, Long userId, Long productId);
    Optional<LoyaltyPointTransaction> findByTypeAndOrder_Id(String type, Long orderId);
    List<LoyaltyPointTransaction> findTop30ByUser_IdOrderByCreatedAtDescIdDesc(Long userId);
}
