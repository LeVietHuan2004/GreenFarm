package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.CouponUsage;
import com.agri.ecommerce.entity.CouponUsageStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCoupon_IdAndUser_IdAndStatusIn(Long couponId, Long userId, Collection<CouponUsageStatus> statuses);
    boolean existsByCoupon_Id(Long couponId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select usage from CouponUsage usage where usage.order.id = :orderId order by usage.coupon.id")
    List<CouponUsage> findAllByOrderIdForUpdate(@Param("orderId") Long orderId);
    List<CouponUsage> findAllByCoupon_IdOrderByCreatedAtDescIdDesc(Long couponId);
}
