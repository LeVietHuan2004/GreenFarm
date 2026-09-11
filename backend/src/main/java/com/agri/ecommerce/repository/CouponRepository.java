package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Coupon;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {
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
