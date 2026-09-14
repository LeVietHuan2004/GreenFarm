package com.agri.ecommerce.repository;

import com.agri.ecommerce.entity.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    Page<Contact> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
    Page<Contact> findAllByStatusOrderByCreatedAtDescIdDesc(String status, Pageable pageable);
    List<Contact> findAllByUser_IdOrderByCreatedAtDescIdDesc(Long userId);
}
