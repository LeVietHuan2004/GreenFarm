package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.OrderStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findAllByOrder_IdOrderByChangedAtAscIdAsc(Long orderId);
}
