package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Payment;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import com.agri.ecommerce.entity.PaymentMethod;
import com.agri.ecommerce.entity.PaymentStatus;
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

    @Query("select payment.id from Payment payment where payment.paymentMethod = :method and payment.status = :status and payment.expiresAt <= :now order by payment.id")
    List<Long> findExpiredIds(@Param("method") PaymentMethod method, @Param("status") PaymentStatus status, @Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    @Query("select payment.id from Payment payment where payment.invoiceEmailSentAt is null and payment.invoiceEmailAttempts < :maxAttempts and payment.invoiceEmailNextRetryAt is not null and payment.invoiceEmailNextRetryAt <= :now and (payment.paymentMethod = :cod or payment.status = :completed) order by payment.invoiceEmailNextRetryAt")
    List<Long> findInvoiceRetryIds(
        @Param("maxAttempts") int maxAttempts,
        @Param("now") LocalDateTime now,
        @Param("cod") PaymentMethod cod,
        @Param("completed") PaymentStatus completed
    );
}
