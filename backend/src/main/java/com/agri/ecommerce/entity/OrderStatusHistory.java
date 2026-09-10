package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private Order order;
    @Convert(converter = OrderStatusConverter.class) private OrderStatus status;
    @Column(name = "changed_at", nullable = false) private LocalDateTime changedAt;
    @Column(columnDefinition = "text") private String note;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){var now=LocalDateTime.now();changedAt=changedAt==null?now:changedAt;createdAt=createdAt==null?now:createdAt;updatedAt=now;}
    public Long getId(){return id;} public void setOrder(Order v){order=v;} public OrderStatus getStatus(){return status;} public void setStatus(OrderStatus v){status=v;}
    public LocalDateTime getChangedAt(){return changedAt;} public String getNote(){return note;} public void setNote(String v){note=v;}
}
