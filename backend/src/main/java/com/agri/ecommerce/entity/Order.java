package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "delivery_staff_id") private User deliveryStaff;
    @Column(nullable = false) private BigDecimal subtotal;
    @Column(name = "discount_amount", nullable = false) private BigDecimal discountAmount;
    @Column(name = "shipping_fee", nullable = false) private BigDecimal shippingFee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "coupon_id") private Coupon coupon;
    @Column(name = "coupon_code") private String couponCode;
    @Column(name = "total_price", nullable = false) private BigDecimal totalPrice;
    @Convert(converter = OrderStatusConverter.class) @Column(nullable = false) private OrderStatus status;
    @Column(name = "dispatched_at") private LocalDateTime dispatchedAt;
    @Column(name = "delivered_at") private LocalDateTime deliveredAt;
    @Column(name = "inventory_released_at") private LocalDateTime inventoryReleasedAt;
    @Column(name = "delivery_failure_reason") private String deliveryFailureReason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shipping_address_id", nullable = false) private ShippingAddress shippingAddress;
    @Column(name = "recipient_name", nullable = false) private String recipientName;
    @Column(name = "recipient_phone", nullable = false) private String recipientPhone;
    @Column(name = "shipping_address_line", nullable = false) private String shippingAddressLine;
    @Column(name = "shipping_city", nullable = false) private String shippingCity;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("id ASC") private List<OrderItem> items = new ArrayList<>();
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) @OrderBy("changedAt ASC, id ASC") private List<OrderStatusHistory> statusHistory = new ArrayList<>();
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){var now=LocalDateTime.now();createdAt=createdAt==null?now:createdAt;updatedAt=now;}
    @PreUpdate void preUpdate(){updatedAt=LocalDateTime.now();}
    public void addItem(OrderItem item){items.add(item);item.setOrder(this);} public void addHistory(OrderStatusHistory history){statusHistory.add(history);history.setOrder(this);}
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;}
    public User getDeliveryStaff(){return deliveryStaff;} public void setDeliveryStaff(User v){deliveryStaff=v;}
    public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal v){subtotal=v;}
    public BigDecimal getDiscountAmount(){return discountAmount;} public void setDiscountAmount(BigDecimal v){discountAmount=v;}
    public BigDecimal getShippingFee(){return shippingFee;} public void setShippingFee(BigDecimal v){shippingFee=v;}
    public Coupon getCoupon(){return coupon;} public void setCoupon(Coupon v){coupon=v;} public String getCouponCode(){return couponCode;} public void setCouponCode(String v){couponCode=v;}
    public BigDecimal getTotalPrice(){return totalPrice;} public void setTotalPrice(BigDecimal v){totalPrice=v;}
    public OrderStatus getStatus(){return status;} public void setStatus(OrderStatus v){status=v;}
    public LocalDateTime getDispatchedAt(){return dispatchedAt;} public void setDispatchedAt(LocalDateTime v){dispatchedAt=v;}
    public LocalDateTime getDeliveredAt(){return deliveredAt;} public void setDeliveredAt(LocalDateTime v){deliveredAt=v;}
    public LocalDateTime getInventoryReleasedAt(){return inventoryReleasedAt;} public void setInventoryReleasedAt(LocalDateTime v){inventoryReleasedAt=v;}
    public String getDeliveryFailureReason(){return deliveryFailureReason;} public void setDeliveryFailureReason(String v){deliveryFailureReason=v;}
    public ShippingAddress getShippingAddress(){return shippingAddress;} public void setShippingAddress(ShippingAddress v){shippingAddress=v;}
    public String getRecipientName(){return recipientName;} public void setRecipientName(String v){recipientName=v;}
    public String getRecipientPhone(){return recipientPhone;} public void setRecipientPhone(String v){recipientPhone=v;}
    public String getShippingAddressLine(){return shippingAddressLine;} public void setShippingAddressLine(String v){shippingAddressLine=v;}
    public String getShippingCity(){return shippingCity;} public void setShippingCity(String v){shippingCity=v;}
    public List<OrderItem> getItems(){return items;} public List<OrderStatusHistory> getStatusHistory(){return statusHistory;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
