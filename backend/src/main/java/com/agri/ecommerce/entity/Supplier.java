package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "supplier_code", nullable = false, unique = true, length = 40) private String supplierCode;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 30) private String phone;
    private String email;
    @Column(length = 500) private String address;
    @Column(nullable = false, length = 20) private String status = "active";
    @Column(length = 1000) private String note;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void created(){LocalDateTime now=LocalDateTime.now();createdAt=now;updatedAt=now;}
    @PreUpdate void updated(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public String getSupplierCode(){return supplierCode;} public void setSupplierCode(String value){supplierCode=value;}
    public String getName(){return name;} public void setName(String value){name=value;}
    public String getPhone(){return phone;} public void setPhone(String value){phone=value;}
    public String getEmail(){return email;} public void setEmail(String value){email=value;}
    public String getAddress(){return address;} public void setAddress(String value){address=value;}
    public String getStatus(){return status;} public void setStatus(String value){status=value;}
    public String getNote(){return note;} public void setNote(String value){note=value;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
