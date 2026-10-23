package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Shipment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrder_Id(Long orderId);
    Optional<Shipment> findByProviderOrderCode(String code);
    Optional<Shipment> findByClientOrderCode(String code);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select shipment from Shipment shipment where shipment.order.id = :orderId")
    Optional<Shipment> findLockedByOrder_Id(@Param("orderId") Long orderId);
}
