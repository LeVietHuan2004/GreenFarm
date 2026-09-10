package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNameIgnoreCase(String name);

    List<Role> findAllByOrderByIdAsc();
}
