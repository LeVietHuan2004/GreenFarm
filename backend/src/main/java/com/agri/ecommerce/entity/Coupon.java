package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
public class Coupon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true) private String code;
    @Enumerated(EnumType.STRING) @Column(name = "coupon_type", nullable = false) private CouponType couponType;
    @Enumerated(EnumType.STRING) @Column(name = "discount_type", nullable = false) private DiscountType discountType;
    @Column(name = "discount_percentage", nullable = false) private int discountPercentage;
    @Column(name = "discount_amount") private BigDecimal discountAmount;
    @Column(name = "starts_at") private LocalDateTime startsAt;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
    @Column(name = "usage_limit") private Integer usageLimit;
    @Column(name = "times_used", nullable = false) private int timesUsed;
    @Column(name = "is_active", nullable = false) private boolean active;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){var now=LocalDateTime.now();createdAt=createdAt==null?now:createdAt;updatedAt=now;}
    @PreUpdate void preUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public String getCode(){return code;} public void setCode(String value){code=value;}
    public CouponType getCouponType(){return couponType;} public void setCouponType(CouponType value){couponType=value;}
    public DiscountType getDiscountType(){return discountType;} public void setDiscountType(DiscountType value){discountType=value;}
    public int getDiscountPercentage(){return discountPercentage;} public BigDecimal getDiscountAmount(){return discountAmount;}
    public void setDiscountPercentage(int value){discountPercentage=value;} public void setDiscountAmount(BigDecimal value){discountAmount=value;}
    public LocalDateTime getStartsAt(){return startsAt;} public LocalDateTime getExpiresAt(){return expiresAt;}
    public void setStartsAt(LocalDateTime value){startsAt=value;} public void setExpiresAt(LocalDateTime value){expiresAt=value;}
    public Integer getUsageLimit(){return usageLimit;} public int getTimesUsed(){return timesUsed;}
    public void setUsageLimit(Integer value){usageLimit=value;} public void setTimesUsed(int value){timesUsed=value;} public boolean isActive(){return active;} public void setActive(boolean value){active=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
