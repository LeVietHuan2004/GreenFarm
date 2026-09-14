package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Review;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findAllByProduct_IdOrderByCreatedAtDescIdDesc(Long productId, Pageable pageable);
    Optional<Review> findByUser_IdAndProduct_Id(Long userId, Long productId);
    Optional<Review> findByIdAndUser_Id(Long id, Long userId);
}
