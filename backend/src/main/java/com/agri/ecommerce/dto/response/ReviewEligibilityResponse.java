package com.agri.ecommerce.dto.response;

public record ReviewEligibilityResponse(boolean purchased, boolean canReview, ReviewResponse existingReview) {}
