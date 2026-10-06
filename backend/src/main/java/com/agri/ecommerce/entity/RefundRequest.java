package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
public class RefundRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private Order order;
    @Column(nullable = false, length = 100) private String reason;
    @Column(length = 1000) private String details;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private RefundRequestStatus status;
    @Column(name = "admin_note", length = 1000) private String adminNote;
    @Column(name = "reviewed_by") private String reviewedBy;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void prePersist(){var now=LocalDateTime.now(); createdAt=now; updatedAt=now;}
    @PreUpdate void preUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public Order getOrder(){return order;} public void setOrder(Order value){order=value;}
    public String getReason(){return reason;} public void setReason(String value){reason=value;}
    public String getDetails(){return details;} public void setDetails(String value){details=value;}
    public RefundRequestStatus getStatus(){return status;} public void setStatus(RefundRequestStatus value){status=value;}
    public String getAdminNote(){return adminNote;} public void setAdminNote(String value){adminNote=value;}
    public String getReviewedBy(){return reviewedBy;} public void setReviewedBy(String value){reviewedBy=value;}
    public LocalDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(LocalDateTime value){reviewedAt=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
