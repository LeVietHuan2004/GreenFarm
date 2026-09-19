package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_point_transactions")
public class LoyaltyPointTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id") private Order order;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "review_id") private Review review;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id") private Product product;
    @Column(nullable = false, length = 30) private String type;
    @Column(nullable = false) private int points;
    @Column(nullable = false, length = 500) private String description;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @PrePersist void prePersist(){if(createdAt==null)createdAt=LocalDateTime.now();}
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User value){user=value;}
    public Order getOrder(){return order;} public void setOrder(Order value){order=value;}
    public Review getReview(){return review;} public void setReview(Review value){review=value;}
    public Product getProduct(){return product;} public void setProduct(Product value){product=value;}
    public String getType(){return type;} public void setType(String value){type=value;}
    public int getPoints(){return points;} public void setPoints(int value){points=value;}
    public String getDescription(){return description;} public void setDescription(String value){description=value;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
