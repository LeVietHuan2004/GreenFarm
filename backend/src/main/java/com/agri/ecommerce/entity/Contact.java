package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contacts")
public class Contact {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(name = "full_name", nullable = false) private String fullName;
    @Column(name = "phone_number") private String phoneNumber;
    private String email;
    @Column(nullable = false, columnDefinition = "text") private String message;
    @Column(name = "is_replied", nullable = false) private boolean replied;
    @Column(nullable = false) private String status = "open";
    @Column(columnDefinition = "text") private String response;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "responded_by") private User respondedBy;
    @Column(name = "responded_at") private LocalDateTime respondedAt;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){var now=LocalDateTime.now();createdAt=createdAt==null?now:createdAt;updatedAt=now;}
    @PreUpdate void preUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;}
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String v){phoneNumber=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getMessage(){return message;} public void setMessage(String v){message=v;}
    public boolean isReplied(){return replied;} public void setReplied(boolean v){replied=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getResponse(){return response;} public void setResponse(String v){response=v;}
    public User getRespondedBy(){return respondedBy;} public void setRespondedBy(User v){respondedBy=v;}
    public LocalDateTime getRespondedAt(){return respondedAt;} public void setRespondedAt(LocalDateTime v){respondedAt=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
