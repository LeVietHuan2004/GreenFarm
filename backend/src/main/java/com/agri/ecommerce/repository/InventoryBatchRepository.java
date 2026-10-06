package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.InventoryBatch;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long>, JpaSpecificationExecutor<InventoryBatch> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select batch from InventoryBatch batch where batch.product.id = :productId order by case when batch.expiryDate is null then 1 else 0 end, batch.expiryDate, batch.id")
    List<InventoryBatch> findByProductIdForUpdate(@Param("productId") Long productId);

    List<InventoryBatch> findAllByProduct_IdOrderByExpiryDateAscIdAsc(Long productId);
    List<InventoryBatch> findAllByOrderByProduct_IdAscIdAsc();

    @Query("select distinct batch.product.id from InventoryBatch batch where batch.expiryDate < :today and batch.remainingQuantity > 0 order by batch.product.id")
    List<Long> findExpiredProductIds(@Param("today") LocalDate today);
}
