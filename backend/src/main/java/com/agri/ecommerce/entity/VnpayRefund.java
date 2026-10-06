package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vnpay_refunds")
public class VnpayRefund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "payment_id", nullable = false) private Payment payment;
    @Column(name = "request_id", nullable = false, unique = true, length = 32) private String requestId;
    @Column(name = "refund_amount", nullable = false) private BigDecimal refundAmount;
    @Column(name = "transaction_type", nullable = false, length = 2) private String transactionType;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "response_code") private String responseCode;
    @Column(name = "transaction_status") private String transactionStatus;
    @Column(name = "refund_transaction_id") private String refundTransactionId;
    @Column(name = "requested_by", nullable = false) private String requestedBy;
    @Column(name = "response_payload", columnDefinition = "text") private String responsePayload;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void created() { LocalDateTime now=LocalDateTime.now(); createdAt=createdAt == null ? now : createdAt; updatedAt=now; }
    @PreUpdate void updated() { updatedAt=LocalDateTime.now(); }
    public Long getId(){return id;} public Payment getPayment(){return payment;} public void setPayment(Payment value){payment=value;}
    public String getRequestId(){return requestId;} public void setRequestId(String value){requestId=value;}
    public BigDecimal getRefundAmount(){return refundAmount;} public void setRefundAmount(BigDecimal value){refundAmount=value;}
    public String getTransactionType(){return transactionType;} public void setTransactionType(String value){transactionType=value;}
    public String getStatus(){return status;} public void setStatus(String value){status=value;}
    public String getResponseCode(){return responseCode;} public void setResponseCode(String value){responseCode=value;}
    public String getTransactionStatus(){return transactionStatus;} public void setTransactionStatus(String value){transactionStatus=value;}
    public String getRefundTransactionId(){return refundTransactionId;} public void setRefundTransactionId(String value){refundTransactionId=value;}
    public String getRequestedBy(){return requestedBy;} public void setRequestedBy(String value){requestedBy=value;}
    public String getResponsePayload(){return responsePayload;} public void setResponsePayload(String value){responsePayload=value;}
}
