package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_batches")
public class InventoryBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "batch_code", nullable = false, unique = true, length = 64) private String batchCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private Product product;
    @Column(nullable = false) private int quantity;
    @Column(name = "remaining_quantity", nullable = false) private int remainingQuantity;
    @Column(name = "reserved_quantity", nullable = false) private int reservedQuantity;
    private String unit;
    @Column(name = "import_price", nullable = false) private BigDecimal importPrice = BigDecimal.ZERO;
    @Column(name = "manufacture_date") private LocalDate manufactureDate;
    @Column(name = "expiry_date") private LocalDate expiryDate;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(name = "imported_at") private LocalDateTime importedAt;
    @Column(length = 1000) private String note;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private User createdBy;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void created(){LocalDateTime now=LocalDateTime.now();if(importedAt==null)importedAt=now;createdAt=now;updatedAt=now;}
    @PreUpdate void updated(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public String getBatchCode(){return batchCode;} public void setBatchCode(String value){batchCode=value;}
    public Product getProduct(){return product;} public void setProduct(Product value){product=value;}
    public int getQuantity(){return quantity;} public void setQuantity(int value){quantity=value;}
    public int getRemainingQuantity(){return remainingQuantity;} public void setRemainingQuantity(int value){remainingQuantity=value;}
    public int getReservedQuantity(){return reservedQuantity;} public void setReservedQuantity(int value){reservedQuantity=value;}
    public String getUnit(){return unit;} public void setUnit(String value){unit=value;}
    public BigDecimal getImportPrice(){return importPrice;} public void setImportPrice(BigDecimal value){importPrice=value;}
    public LocalDate getManufactureDate(){return manufactureDate;} public void setManufactureDate(LocalDate value){manufactureDate=value;}
    public LocalDate getExpiryDate(){return expiryDate;} public void setExpiryDate(LocalDate value){expiryDate=value;}
    public Supplier getSupplier(){return supplier;} public void setSupplier(Supplier value){supplier=value;}
    public LocalDateTime getImportedAt(){return importedAt;} public void setImportedAt(LocalDateTime value){importedAt=value;}
    public String getNote(){return note;} public void setNote(String value){note=value;}
    public User getCreatedBy(){return createdBy;} public void setCreatedBy(User value){createdBy=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
