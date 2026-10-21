package csit314.renovation.review.dto;

import java.util.List;
import java.util.Map;

public record PublicDesignerReviewsResponse(long totalReviews, double averageRating,
        Map<Integer, Long> ratingDistribution, List<PublicDesignerReviewResponse> reviews,
        int page, boolean hasMore) {}
