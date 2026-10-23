package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.ShipmentEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
    boolean existsByShipment_IdAndEventKey(Long shipmentId, String eventKey);
    List<ShipmentEvent> findAllByShipment_IdOrderByEventAtAscIdAsc(Long shipmentId);
}
