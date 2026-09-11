package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;

public record DeliveryAssignmentRequest(@NotNull Long deliveryStaffId) {}
