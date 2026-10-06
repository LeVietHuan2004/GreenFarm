package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private Order order;
    @Convert(converter = PaymentMethodConverter.class) @Column(name = "payment_method", nullable = false) private PaymentMethod paymentMethod;
    @Column(name = "reference_code") private String referenceCode;
    @Column(name = "transaction_id") private String transactionId;
    @Column(nullable = false) private BigDecimal amount;
    @Convert(converter = PaymentStatusConverter.class) @Column(nullable = false) private PaymentStatus status;
    @Column(name = "gateway_response_code") private String gatewayResponseCode;
    @Column(name = "gateway_payload", columnDefinition = "text") private String gatewayPayload;
    @Column(name = "refund_request_id") private String refundRequestId;
    @Column(name = "refund_gateway_transaction_id") private String refundGatewayTransactionId;
    @Column(name = "refund_requested_at") private LocalDateTime refundRequestedAt;
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(name = "invoice_email_sent_at") private LocalDateTime invoiceEmailSentAt;
    @Column(name = "invoice_email_attempts", nullable = false) private int invoiceEmailAttempts;
    @Column(name = "invoice_email_next_retry_at") private LocalDateTime invoiceEmailNextRetryAt;
    @Column(name = "invoice_email_last_error") private String invoiceEmailLastError;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
    @Column(name = "vnpay_payment_request_date") private LocalDateTime vnpayPaymentRequestDate;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist void prePersist() { var now = LocalDateTime.now(); createdAt = createdAt == null ? now : createdAt; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Order getOrder() { return order; } public void setOrder(Order value) { order = value; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(PaymentMethod value) { paymentMethod = value; }
    public String getReferenceCode() { return referenceCode; } public void setReferenceCode(String value) { referenceCode = value; }
    public String getTransactionId() { return transactionId; } public void setTransactionId(String value) { transactionId = value; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal value) { amount = value; }
    public PaymentStatus getStatus() { return status; } public void setStatus(PaymentStatus value) { status = value; }
    public String getGatewayResponseCode() { return gatewayResponseCode; } public void setGatewayResponseCode(String value) { gatewayResponseCode = value; }
    public String getGatewayPayload() { return gatewayPayload; } public void setGatewayPayload(String value) { gatewayPayload = value; }
    public String getRefundRequestId() { return refundRequestId; } public void setRefundRequestId(String value) { refundRequestId = value; }
    public String getRefundGatewayTransactionId() { return refundGatewayTransactionId; } public void setRefundGatewayTransactionId(String value) { refundGatewayTransactionId = value; }
    public LocalDateTime getRefundRequestedAt() { return refundRequestedAt; } public void setRefundRequestedAt(LocalDateTime value) { refundRequestedAt = value; }
    public LocalDateTime getPaidAt() { return paidAt; } public void setPaidAt(LocalDateTime value) { paidAt = value; }
    public LocalDateTime getInvoiceEmailSentAt() { return invoiceEmailSentAt; } public void setInvoiceEmailSentAt(LocalDateTime value) { invoiceEmailSentAt = value; }
    public int getInvoiceEmailAttempts() { return invoiceEmailAttempts; } public void setInvoiceEmailAttempts(int value) { invoiceEmailAttempts = value; }
    public LocalDateTime getInvoiceEmailNextRetryAt() { return invoiceEmailNextRetryAt; } public void setInvoiceEmailNextRetryAt(LocalDateTime value) { invoiceEmailNextRetryAt = value; }
    public String getInvoiceEmailLastError() { return invoiceEmailLastError; } public void setInvoiceEmailLastError(String value) { invoiceEmailLastError = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; } public void setExpiresAt(LocalDateTime value) { expiresAt = value; }
    public LocalDateTime getVnpayPaymentRequestDate() { return vnpayPaymentRequestDate; } public void setVnpayPaymentRequestDate(LocalDateTime value) { vnpayPaymentRequestDate = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
