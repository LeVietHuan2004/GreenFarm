package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.OrderInventoryAllocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderInventoryAllocationRepository extends JpaRepository<OrderInventoryAllocation, Long> {
    List<OrderInventoryAllocation> findAllByOrderItem_Order_IdOrderByIdAsc(Long orderId);
}
