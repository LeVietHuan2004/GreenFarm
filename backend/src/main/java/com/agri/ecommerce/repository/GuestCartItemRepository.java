package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.GuestCartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestCartItemRepository extends JpaRepository<GuestCartItem,Long> {
    @EntityGraph(attributePaths={"product","product.category","product.images"})
    List<GuestCartItem> findAllByGuestSession_IdOrderByCreatedAtDescIdDesc(Long sessionId);
    Optional<GuestCartItem> findByGuestSession_IdAndProduct_Id(Long sessionId,Long productId);
    Optional<GuestCartItem> findByIdAndGuestSession_Id(Long id,Long sessionId);
    void deleteAllByGuestSession_Id(Long sessionId);
}
