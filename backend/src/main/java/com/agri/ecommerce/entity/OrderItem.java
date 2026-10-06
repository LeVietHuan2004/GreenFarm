package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private Order order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "product_id", nullable = false) private Product product;
    @Column(name = "product_name", nullable = false) private String productName;
    @Column(name = "product_unit") private String productUnit;
    @Column(name = "product_image", length = 1024) private String productImage;
    @Column(nullable = false) private int quantity;
    @Column(nullable = false) private BigDecimal price;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){var now=LocalDateTime.now();createdAt=createdAt==null?now:createdAt;updatedAt=now;}
    @PreUpdate void preUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public void setOrder(Order v){order=v;} public Product getProduct(){return product;} public void setProduct(Product v){product=v;}
    public String getProductName(){return productName;} public void setProductName(String v){productName=v;}
    public String getProductUnit(){return productUnit;} public void setProductUnit(String v){productUnit=v;}
    public String getProductImage(){return productImage;} public void setProductImage(String v){productImage=v;}
    public int getQuantity(){return quantity;} public void setQuantity(int v){quantity=v;} public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;}
}
