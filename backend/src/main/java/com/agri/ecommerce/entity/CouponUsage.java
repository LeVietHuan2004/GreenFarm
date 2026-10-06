package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_usages")
public class CouponUsage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "coupon_id", nullable = false) private Coupon coupon;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "guest_session_id") private GuestSession guestSession;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private Order order;
    @Column(name = "discount_amount", nullable = false) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CouponUsageStatus status;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "used_at") private LocalDateTime usedAt;
    @Column(name = "released_at") private LocalDateTime releasedAt;
    @PrePersist void prePersist(){if(createdAt==null)createdAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public Coupon getCoupon(){return coupon;} public void setCoupon(Coupon value){coupon=value;}
    public User getUser(){return user;} public void setUser(User value){user=value;}
    public GuestSession getGuestSession(){return guestSession;} public void setGuestSession(GuestSession value){guestSession=value;}
    public Order getOrder(){return order;} public void setOrder(Order value){order=value;}
    public BigDecimal getDiscountAmount(){return discountAmount;} public void setDiscountAmount(BigDecimal value){discountAmount=value;}
    public CouponUsageStatus getStatus(){return status;} public void setStatus(CouponUsageStatus value){status=value;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getUsedAt(){return usedAt;} public void setUsedAt(LocalDateTime value){usedAt=value;}
    public LocalDateTime getReleasedAt(){return releasedAt;} public void setReleasedAt(LocalDateTime value){releasedAt=value;}
}
