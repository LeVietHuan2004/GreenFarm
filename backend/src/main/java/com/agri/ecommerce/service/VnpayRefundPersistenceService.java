package com.agri.ecommerce.service;

import com.agri.ecommerce.common.exception.ApplicationException;
import com.agri.ecommerce.entity.*;
import com.agri.ecommerce.repository.PaymentRepository;
import com.agri.ecommerce.repository.VnpayRefundRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VnpayRefundPersistenceService {
    private static final List<String> IN_FLIGHT = List.of("PENDING", "PROCESSING");
    private final PaymentRepository payments;
    private final VnpayRefundRepository refunds;
    public VnpayRefundPersistenceService(PaymentRepository payments, VnpayRefundRepository refunds) { this.payments=payments; this.refunds=refunds; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RefundAttempt prepare(Long orderId, BigDecimal requestedAmount, String actor) {
        Payment payment = payments.findByOrder_Id(orderId).orElseThrow(() -> conflict("PAYMENT_NOT_FOUND", "Khong tim thay giao dich cua don hang"));
        payment = payments.findByIdForUpdate(payment.getId()).orElseThrow();
        if (payment.getPaymentMethod() != PaymentMethod.VNPAY) return RefundAttempt.notGateway(payment);
        if (payment.getStatus() == PaymentStatus.REFUNDED) return RefundAttempt.alreadyRefunded(payment);
        if (payment.getStatus() != PaymentStatus.COMPLETED) throw conflict("PAYMENT_NOT_COMPLETED", "Chi co the hoan giao dich VNPAY da thanh toan");
        if (payment.getTransactionId() == null || payment.getCreatedAt() == null) throw conflict("VNPAY_TRANSACTION_MISSING", "Giao dich thieu ma hoac thoi gian tao VNPAY");
        VnpayRefund inFlight = refunds.findFirstByPayment_IdAndStatusInOrderByIdDesc(payment.getId(), IN_FLIGHT).orElse(null);
        if (inFlight != null) return RefundAttempt.inFlight(payment, inFlight);
        BigDecimal completed = refunds.completedAmount(payment.getId());
        BigDecimal remaining = payment.getAmount().subtract(completed);
        if (remaining.signum() <= 0) throw conflict("REFUND_ALREADY_COMPLETED", "Giao dich da duoc hoan du tien");
        BigDecimal amount = requestedAmount == null ? remaining : requestedAmount;
        if (amount.signum() <= 0 || amount.compareTo(remaining) > 0) throw conflict("INVALID_REFUND_AMOUNT", "So tien hoan phai lon hon 0 va khong vuot so tien con lai");
        VnpayRefund refund = new VnpayRefund();
        refund.setPayment(payment); refund.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        refund.setRefundAmount(amount); refund.setTransactionType(amount.compareTo(remaining) == 0 ? "02" : "03");
        refund.setStatus("PENDING"); refund.setRequestedBy(actor == null || actor.isBlank() ? "admin" : actor.substring(0, Math.min(actor.length(), 245)));
        refunds.save(refund);
        payment.setRefundRequestId(refund.getRequestId()); payment.setRefundRequestedAt(LocalDateTime.now()); payments.save(payment);
        return RefundAttempt.ready(payment, refund);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordResponse(Long refundId, String responseCode, String transactionStatus, String transactionId, String payload, boolean signatureValid) {
        VnpayRefund refund = refunds.findById(refundId).orElseThrow();
        Payment payment = payments.findByIdForUpdate(refund.getPayment().getId()).orElseThrow();
        refund.setResponseCode(responseCode); refund.setTransactionStatus(transactionStatus); refund.setRefundTransactionId(transactionId); refund.setResponsePayload(payload);
        if (!signatureValid) refund.setStatus("INVALID_SIGNATURE");
        else if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
            refund.setStatus("SUCCESS");
            if ("02".equals(refund.getTransactionType())) payment.setStatus(PaymentStatus.REFUNDED);
        } else if ("00".equals(responseCode) || "94".equals(responseCode) || "05".equals(transactionStatus) || "06".equals(transactionStatus)) refund.setStatus("PROCESSING");
        else refund.setStatus("FAILED");
        payment.setGatewayResponseCode("REFUND_" + (responseCode == null ? "UNKNOWN" : responseCode));
        payment.setRefundGatewayTransactionId(transactionId); payment.setGatewayPayload(payload);
        refunds.save(refund); payments.save(payment);
    }

    private ApplicationException conflict(String code, String message) { return new ApplicationException(HttpStatus.CONFLICT, code, message); }
    public record RefundAttempt(Payment payment, VnpayRefund refund, boolean gatewayRequired, boolean alreadyFinal, boolean processing) {
        static RefundAttempt ready(Payment payment, VnpayRefund refund) { return new RefundAttempt(payment, refund, true, false, false); }
        static RefundAttempt inFlight(Payment payment, VnpayRefund refund) { return new RefundAttempt(payment, refund, false, false, true); }
        static RefundAttempt notGateway(Payment payment) { return new RefundAttempt(payment, null, false, false, false); }
        static RefundAttempt alreadyRefunded(Payment payment) { return new RefundAttempt(payment, null, false, true, false); }
    }
}
