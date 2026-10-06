package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.VnpayRefund;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VnpayRefundRepository extends JpaRepository<VnpayRefund, Long> {
    Optional<VnpayRefund> findFirstByPayment_IdAndStatusInOrderByIdDesc(Long paymentId, Collection<String> statuses);
    @Query("select coalesce(sum(refund.refundAmount), 0) from VnpayRefund refund where refund.payment.id=:paymentId and refund.status='SUCCESS'")
    BigDecimal completedAmount(@Param("paymentId") Long paymentId);
}
