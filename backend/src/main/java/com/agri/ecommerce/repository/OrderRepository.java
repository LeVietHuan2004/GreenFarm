package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByUser_IdOrderByCreatedAtDescIdDesc(Long userId);
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUser_Id(Long id, Long userId);
    boolean existsByShippingAddress_Id(Long addressId);
}
