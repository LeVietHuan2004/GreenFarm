package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.ShippingAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {
    List<ShippingAddress> findAllByUser_IdOrderByDefaultAddressDescCreatedAtDesc(Long userId);
    Optional<ShippingAddress> findByIdAndUser_Id(Long id, Long userId);
    long countByUser_Id(Long userId);
    List<ShippingAddress> findAllByUser_IdAndDefaultAddressTrue(Long userId);
}
