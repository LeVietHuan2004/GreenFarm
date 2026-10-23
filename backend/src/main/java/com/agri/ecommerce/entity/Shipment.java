package com.agri.ecommerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
public class Shipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id", nullable = false) private Order order;
    @Column(nullable = false, length = 20) private String provider = "ghn";
    @Column(name = "provider_order_code", length = 64) private String providerOrderCode;
    @Column(name = "client_order_code", nullable = false, length = 50) private String clientOrderCode;
    @Column(name = "shop_id", nullable = false) private int shopId;
    @Column(name = "customer_shipping_fee", nullable = false) private BigDecimal customerShippingFee = BigDecimal.ZERO;
    @Column(name = "carrier_shipping_fee") private BigDecimal carrierShippingFee;
    @Column(name = "cod_amount", nullable = false) private BigDecimal codAmount = BigDecimal.ZERO;
    @Column(name = "shipping_status", nullable = false, length = 50) private String shippingStatus = "not_created";
    @Column(name = "shipping_service_type") private Integer shippingServiceType;
    @Column(name = "estimated_delivery_at") private LocalDateTime estimatedDeliveryAt;
    @Column(name = "last_event_at") private LocalDateTime lastEventAt;
    @Column(name = "cod_transferred_at") private LocalDateTime codTransferredAt;
    @Column(name = "last_error", length = 1000) private String lastError;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void prePersist(){var now=LocalDateTime.now();createdAt=now;updatedAt=now;}
    @PreUpdate void preUpdate(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public Order getOrder(){return order;} public void setOrder(Order v){order=v;}
    public String getProvider(){return provider;} public void setProvider(String v){provider=v;}
    public String getProviderOrderCode(){return providerOrderCode;} public void setProviderOrderCode(String v){providerOrderCode=v;}
    public String getClientOrderCode(){return clientOrderCode;} public void setClientOrderCode(String v){clientOrderCode=v;}
    public int getShopId(){return shopId;} public void setShopId(int v){shopId=v;}
    public BigDecimal getCustomerShippingFee(){return customerShippingFee;} public void setCustomerShippingFee(BigDecimal v){customerShippingFee=v;}
    public BigDecimal getCarrierShippingFee(){return carrierShippingFee;} public void setCarrierShippingFee(BigDecimal v){carrierShippingFee=v;}
    public BigDecimal getCodAmount(){return codAmount;} public void setCodAmount(BigDecimal v){codAmount=v;}
    public String getShippingStatus(){return shippingStatus;} public void setShippingStatus(String v){shippingStatus=v;}
    public Integer getShippingServiceType(){return shippingServiceType;} public void setShippingServiceType(Integer v){shippingServiceType=v;}
    public LocalDateTime getEstimatedDeliveryAt(){return estimatedDeliveryAt;} public void setEstimatedDeliveryAt(LocalDateTime v){estimatedDeliveryAt=v;}
    public LocalDateTime getLastEventAt(){return lastEventAt;} public void setLastEventAt(LocalDateTime v){lastEventAt=v;}
    public LocalDateTime getCodTransferredAt(){return codTransferredAt;} public void setCodTransferredAt(LocalDateTime v){codTransferredAt=v;}
    public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
