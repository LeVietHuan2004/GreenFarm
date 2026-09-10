package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String code;
    @Column(name = "coupon_type", nullable = false) private String couponType;
    @Column(name = "discount_type", nullable = false) private String discountType;
    @Column(name = "discount_percentage", nullable = false) private int discountPercentage;
    @Column(name = "discount_amount") private BigDecimal discountAmount;
    @Column(name = "starts_at") private LocalDateTime startsAt;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
    @Column(name = "usage_limit") private Integer usageLimit;
    @Column(name = "times_used", nullable = false) private int timesUsed;
    @Column(name = "is_active", nullable = false) private boolean active;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    public Long getId(){return id;} public String getCode(){return code;}
    public String getCouponType(){return couponType;} public String getDiscountType(){return discountType;}
    public int getDiscountPercentage(){return discountPercentage;} public BigDecimal getDiscountAmount(){return discountAmount;}
    public LocalDateTime getStartsAt(){return startsAt;} public LocalDateTime getExpiresAt(){return expiresAt;}
    public Integer getUsageLimit(){return usageLimit;} public int getTimesUsed(){return timesUsed;}
    public void setTimesUsed(int value){timesUsed=value;} public boolean isActive(){return active;}
}
