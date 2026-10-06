package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record SupplierResponse(Long id, String supplierCode, String name, String phone, String email,
                               String address, String status, String note, LocalDateTime createdAt, LocalDateTime updatedAt) {}
