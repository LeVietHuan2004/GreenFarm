package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Product;
import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id in :ids order by product.id")
    List<Product> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"category", "images"})
    Optional<Product> findBySlugIgnoreCase(String slug);

    @EntityGraph(attributePaths = {"category", "images"})
    @Override
    Optional<Product> findById(Long id);

    boolean existsByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    long countByCategoryIdAndStatusNot(Long categoryId, com.agri.ecommerce.entity.ProductStatus status);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

    @EntityGraph(attributePaths = {"category", "images"})
    List<Product> findTop50ByStatusNotOrderByUpdatedAtDesc(com.agri.ecommerce.entity.ProductStatus status);
}
