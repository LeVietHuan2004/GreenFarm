package com.agri.ecommerce.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record LoyaltySummaryResponse(int pointsBalance, BigDecimal discountPerPoint, int earnAmountPerPoint,
                                     int maxRedemptionPercent, List<LoyaltyPointTransactionResponse> transactions) {}
