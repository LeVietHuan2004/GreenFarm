package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.User;
import com.agri.ecommerce.entity.UserStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    // Serialize commerce writes even when a user's cart or wishlist is empty.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :userId")
    Optional<User> findByIdForCommerceUpdate(@Param("userId") Long userId);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("select user from User user where lower(user.role.name) = lower(:role) and user.status = :status")
    List<User> findAllByRoleAndStatus(@Param("role") String role, @Param("status") UserStatus status);

    @Query("""
        select user from User user
        where (
            :search is null
            or lower(user.name) like lower(concat('%', :search, '%'))
            or lower(user.email) like lower(concat('%', :search, '%'))
        )
        and (:role is null or lower(user.role.name) = lower(:role))
        and (:status is null or user.status = :status)
        """)
    Page<User> search(
        @Param("search") String search,
        @Param("role") String role,
        @Param("status") UserStatus status,
        Pageable pageable
    );
}
