package com.agri.ecommerce.dto.response;
public record GuestOrderCreatedResponse(OrderResponse order,String lookupToken,boolean replayed,String paymentUrl) {}
