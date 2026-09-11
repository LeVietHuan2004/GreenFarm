package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryStatusUpdateRequest(@NotBlank String status, @Size(max = 500) String note) {}
