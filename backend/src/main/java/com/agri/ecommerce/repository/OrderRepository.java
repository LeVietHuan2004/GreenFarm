package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByUser_IdOrderByCreatedAtDescIdDesc(Long userId);
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUser_Id(Long id, Long userId);
    Page<Order> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
    Page<Order> findAllByStatusOrderByCreatedAtDescIdDesc(com.agri.ecommerce.entity.OrderStatus status, Pageable pageable);
    List<Order> findAllByStatusInOrderByCreatedAtDescIdDesc(List<com.agri.ecommerce.entity.OrderStatus> statuses);
    List<Order> findAllByDeliveryStaff_IdOrderByCreatedAtDescIdDesc(Long deliveryStaffId);
    @Query("""
        select (count(purchaseOrder) > 0) from Order purchaseOrder
        join purchaseOrder.items item
        where purchaseOrder.user.id = :userId
          and item.product.id = :productId
          and purchaseOrder.status in :statuses
        """)
    boolean hasPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId,
                                @Param("statuses") List<com.agri.ecommerce.entity.OrderStatus> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select purchaseOrder from Order purchaseOrder where purchaseOrder.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select purchaseOrder from Order purchaseOrder where purchaseOrder.id = :id and purchaseOrder.user.id = :userId")
    Optional<Order> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
    boolean existsByShippingAddress_Id(Long addressId);
}
