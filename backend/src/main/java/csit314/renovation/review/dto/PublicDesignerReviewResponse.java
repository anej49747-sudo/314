package csit314.renovation.review.dto;

import csit314.renovation.review.entity.DesignerReview;
import java.time.Instant;

/** Public review fields deliberately exclude customer identity and account IDs. */
public record PublicDesignerReviewResponse(int rating, String comment, Instant createdAt) {
    public static PublicDesignerReviewResponse from(DesignerReview review) {
        return new PublicDesignerReviewResponse(review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
