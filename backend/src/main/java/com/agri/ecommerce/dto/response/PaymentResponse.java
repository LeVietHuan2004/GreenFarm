package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(Long id, String method, String status, BigDecimal amount, String referenceCode,
                              String transactionId, LocalDateTime paidAt, String paymentUrl) {}
