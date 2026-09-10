package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder_Id(Long orderId);
    Optional<Payment> findByReferenceCode(String referenceCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.referenceCode = :referenceCode")
    Optional<Payment> findByReferenceCodeForUpdate(@Param("referenceCode") String referenceCode);
}
