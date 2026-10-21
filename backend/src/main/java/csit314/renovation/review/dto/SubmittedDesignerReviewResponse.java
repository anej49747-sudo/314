package csit314.renovation.review.dto;

import csit314.renovation.review.entity.DesignerReview;
import java.time.Instant;

/** Submission acknowledgement only. Story #10 will define the public review list DTO. */
public record SubmittedDesignerReviewResponse(Long id, int rating, String comment, Instant createdAt) {
    public static SubmittedDesignerReviewResponse from(DesignerReview review) {
        return new SubmittedDesignerReviewResponse(review.getId(), review.getRating(),
                review.getComment(), review.getCreatedAt());
    }
}
