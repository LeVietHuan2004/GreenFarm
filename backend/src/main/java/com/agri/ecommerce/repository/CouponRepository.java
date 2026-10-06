package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Coupon;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {
    @Query("select coupon from Coupon coupon where coupon.active = true "
        + "and (coupon.startsAt is null or coupon.startsAt <= :now) "
        + "and (coupon.expiresAt is null or coupon.expiresAt >= :now) "
        + "and (coupon.usageLimit is null or coupon.timesUsed < coupon.usageLimit) "
        + "order by coupon.couponType, coupon.code")
    List<Coupon> findPubliclyAvailable(@Param("now") LocalDateTime now);

    Optional<Coupon> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select coupon from Coupon coupon where lower(coupon.code) = lower(:code)")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select coupon from Coupon coupon where coupon.id = :id")
    Optional<Coupon> findByIdForUpdate(@Param("id") Long id);
}
