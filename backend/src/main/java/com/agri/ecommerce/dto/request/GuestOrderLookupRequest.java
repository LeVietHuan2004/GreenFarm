package com.agri.ecommerce.dto.request;
import jakarta.validation.constraints.*;
public record GuestOrderLookupRequest(@NotNull @Positive Long orderId,@NotBlank @Email String email,@NotBlank @Size(min=32,max=128) String token) {}
