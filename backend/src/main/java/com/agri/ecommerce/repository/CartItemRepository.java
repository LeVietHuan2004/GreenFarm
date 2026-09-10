package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = {"product", "product.category", "product.images"})
    List<CartItem> findAllByUser_IdOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"product", "product.category", "product.images"})
    Optional<CartItem> findByUser_IdAndProduct_Id(Long userId, Long productId);

    @EntityGraph(attributePaths = {"product", "product.category", "product.images"})
    Optional<CartItem> findByIdAndUser_Id(Long itemId, Long userId);

    void deleteAllByUser_Id(Long userId);
}
