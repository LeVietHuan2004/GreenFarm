package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="guest_cart_items", uniqueConstraints=@UniqueConstraint(name="uk_guest_cart_session_product", columnNames={"guest_session_id","product_id"}))
public class GuestCartItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="guest_session_id", nullable=false) private GuestSession guestSession;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="product_id", nullable=false) private Product product;
    @Column(nullable=false) private int quantity;
    @Column(name="created_at") private LocalDateTime createdAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){var now=LocalDateTime.now();createdAt=createdAt==null?now:createdAt;updatedAt=now;}
    @PreUpdate void preUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public GuestSession getGuestSession(){return guestSession;} public void setGuestSession(GuestSession value){guestSession=value;}
    public Product getProduct(){return product;} public void setProduct(Product value){product=value;}
    public int getQuantity(){return quantity;} public void setQuantity(int value){quantity=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
