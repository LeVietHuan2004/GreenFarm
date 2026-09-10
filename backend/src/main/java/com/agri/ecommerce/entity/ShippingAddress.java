package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_addresses")
public class ShippingAddress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "full_name", nullable = false) private String fullName;
    @Column(nullable = false) private String phone;
    @Column(nullable = false) private String address;
    @Column(nullable = false) private String city;
    @Column(name = "`default`", nullable = false) private boolean defaultAddress;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){ var now=LocalDateTime.now(); createdAt=createdAt==null?now:createdAt; updatedAt=now; }
    @PreUpdate void preUpdate(){ updatedAt=LocalDateTime.now(); }
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;}
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getCity(){return city;} public void setCity(String v){city=v;}
    public boolean isDefaultAddress(){return defaultAddress;} public void setDefaultAddress(boolean v){defaultAddress=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
