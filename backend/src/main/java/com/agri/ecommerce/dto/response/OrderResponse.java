package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(Long id, String status, BigDecimal subtotal, BigDecimal shippingFee,
                            BigDecimal discountAmount, BigDecimal total, String couponCode,
                            String recipientName, String recipientPhone, String shippingAddress,
                            String shippingCity, List<OrderItemResponse> items,
                            List<OrderStatusHistoryResponse> statusHistory, LocalDateTime createdAt,
                            LocalDateTime updatedAt, PaymentResponse payment, Long deliveryStaffId,
                            String deliveryStaffName, LocalDateTime deliveryClaimedAt,
                            String deliveryFailureReason) {}
