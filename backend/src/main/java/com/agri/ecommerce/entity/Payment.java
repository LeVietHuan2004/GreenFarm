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
    @Column(name = "paid_at") private LocalDateTime paidAt;
    @Column(name = "invoice_email_sent_at") private LocalDateTime invoiceEmailSentAt;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
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
    public LocalDateTime getPaidAt() { return paidAt; } public void setPaidAt(LocalDateTime value) { paidAt = value; }
    public LocalDateTime getInvoiceEmailSentAt() { return invoiceEmailSentAt; } public void setInvoiceEmailSentAt(LocalDateTime value) { invoiceEmailSentAt = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; } public void setExpiresAt(LocalDateTime value) { expiresAt = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
