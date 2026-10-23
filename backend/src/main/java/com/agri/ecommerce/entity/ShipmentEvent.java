package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_events")
public class ShipmentEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "shipment_id", nullable = false) private Shipment shipment;
    @Column(name = "event_key", nullable = false, length = 150) private String eventKey;
    @Column(name = "event_type", nullable = false, length = 50) private String eventType;
    @Column(name = "shipping_status", length = 50) private String shippingStatus;
    @Column(name = "event_at") private LocalDateTime eventAt;
    @Column(length = 1000) private String description;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @PrePersist void prePersist(){createdAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public Shipment getShipment(){return shipment;} public void setShipment(Shipment v){shipment=v;}
    public String getEventKey(){return eventKey;} public void setEventKey(String v){eventKey=v;}
    public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
    public String getShippingStatus(){return shippingStatus;} public void setShippingStatus(String v){shippingStatus=v;}
    public LocalDateTime getEventAt(){return eventAt;} public void setEventAt(LocalDateTime v){eventAt=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
