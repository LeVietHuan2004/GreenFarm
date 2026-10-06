package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_inventory_allocations", uniqueConstraints = @UniqueConstraint(columnNames = {"order_item_id", "batch_id"}))
public class OrderInventoryAllocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_item_id", nullable = false) private OrderItem orderItem;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "batch_id", nullable = false) private InventoryBatch batch;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "exported_at") private LocalDateTime exportedAt;
    @Column(name = "restored_at") private LocalDateTime restoredAt;
    @PrePersist void created(){if(createdAt==null)createdAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public OrderItem getOrderItem(){return orderItem;} public void setOrderItem(OrderItem value){orderItem=value;}
    public InventoryBatch getBatch(){return batch;} public void setBatch(InventoryBatch value){batch=value;}
    public int getQuantity(){return quantity;} public void setQuantity(int value){quantity=value;}
    public String getStatus(){return status;} public void setStatus(String value){status=value;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getExportedAt(){return exportedAt;} public void setExportedAt(LocalDateTime value){exportedAt=value;}
    public LocalDateTime getRestoredAt(){return restoredAt;} public void setRestoredAt(LocalDateTime value){restoredAt=value;}
}
