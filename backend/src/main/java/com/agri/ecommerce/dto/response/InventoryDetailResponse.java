package com.agri.ecommerce.dto.response;

import java.util.List;

public record InventoryDetailResponse(InventoryProductResponse product, List<InventoryBatchResponse> batches) {}
