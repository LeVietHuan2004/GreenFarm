package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.RefundRequest;
import com.agri.ecommerce.entity.RefundRequestStatus;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    Optional<RefundRequest> findFirstByOrder_IdOrderByIdDesc(Long orderId);
    Optional<RefundRequest> findFirstByOrder_IdAndStatusOrderByIdDesc(Long orderId, RefundRequestStatus status);
    boolean existsByOrder_IdAndStatusIn(Long orderId, Collection<RefundRequestStatus> statuses);
    Page<RefundRequest> findAllByStatusOrderByCreatedAtDesc(RefundRequestStatus status, Pageable pageable);
    Page<RefundRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from RefundRequest request join fetch request.order where request.id=:id")
    Optional<RefundRequest> findByIdForUpdate(@Param("id") Long id);
}
