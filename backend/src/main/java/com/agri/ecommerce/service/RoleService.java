package com.agri.ecommerce.service;

import com.agri.ecommerce.dto.response.RoleResponse;
import com.agri.ecommerce.mapper.UserMapper;
import com.agri.ecommerce.repository.RoleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleRepository.findAllByOrderByIdAsc().stream()
            .map(UserMapper::toResponse)
            .toList();
    }
}
