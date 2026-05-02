package com.ecommerce.product.dto;

import java.util.List;

public record ReviewSummaryResponse(
        double averageRating,
        int totalCount,
        List<ReviewResponse> last5,
        ReviewResponse myReview
) {}
