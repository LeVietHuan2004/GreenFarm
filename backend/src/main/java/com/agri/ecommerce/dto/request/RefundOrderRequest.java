package com.agri.ecommerce.dto.request;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record RefundOrderRequest(
    @Size(max = 500) String note,
    @DecimalMin(value = "0.01", message = "So tien hoan phai lon hon 0")
    @Digits(integer = 13, fraction = 2, message = "So tien hoan khong hop le")
    BigDecimal amount
) {}
