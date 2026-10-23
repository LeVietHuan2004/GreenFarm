package com.agri.ecommerce.dto.response;

import java.time.LocalDateTime;

public record ShippingAddressResponse(Long id, String fullName, String phone, String address, String city,
                                      boolean defaultAddress, LocalDateTime createdAt, LocalDateTime updatedAt,
                                      Integer ghnProvinceId, String ghnProvinceName, Integer ghnWardId, String ghnWardName) {}
