package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Wishlist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @EntityGraph(attributePaths = {"product", "product.category", "product.images"})
    List<Wishlist> findAllByUser_IdOrderByCreatedAtDescIdDesc(Long userId);

    @EntityGraph(attributePaths = {"product", "product.category", "product.images"})
    Optional<Wishlist> findByUser_IdAndProduct_Id(Long userId, Long productId);
}
