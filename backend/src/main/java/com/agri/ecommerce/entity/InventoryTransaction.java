package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private Product product;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "batch_id", nullable = false) private InventoryBatch batch;
    @Column(nullable = false, length = 30) private String type;
    @Column(nullable = false) private int quantity;
    @Column(name = "quantity_before", nullable = false) private int quantityBefore;
    @Column(name = "quantity_after", nullable = false) private int quantityAfter;
    @Column(name = "reserved_before", nullable = false) private int reservedBefore;
    @Column(name = "reserved_after", nullable = false) private int reservedAfter;
    @Column(name = "reference_id") private Long referenceId;
    @Column(length = 1000) private String reason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private User createdBy;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @PrePersist void created(){if(createdAt==null)createdAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public Product getProduct(){return product;} public void setProduct(Product value){product=value;}
    public InventoryBatch getBatch(){return batch;} public void setBatch(InventoryBatch value){batch=value;}
    public String getType(){return type;} public void setType(String value){type=value;}
    public int getQuantity(){return quantity;} public void setQuantity(int value){quantity=value;}
    public int getQuantityBefore(){return quantityBefore;} public void setQuantityBefore(int value){quantityBefore=value;}
    public int getQuantityAfter(){return quantityAfter;} public void setQuantityAfter(int value){quantityAfter=value;}
    public int getReservedBefore(){return reservedBefore;} public void setReservedBefore(int value){reservedBefore=value;}
    public int getReservedAfter(){return reservedAfter;} public void setReservedAfter(int value){reservedAfter=value;}
    public Long getReferenceId(){return referenceId;} public void setReferenceId(Long value){referenceId=value;}
    public String getReason(){return reason;} public void setReason(String value){reason=value;}
    public User getCreatedBy(){return createdBy;} public void setCreatedBy(User value){createdBy=value;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
