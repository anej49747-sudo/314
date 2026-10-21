package csit314.renovation.review.control;

import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.review.dto.PublicDesignerReviewResponse;
import csit314.renovation.review.dto.PublicDesignerReviewsResponse;
import csit314.renovation.review.entity.DesignerReview;
import csit314.renovation.review.repository.DesignerReviewRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicDesignerReviewService {
    private static final int PAGE_SIZE = 3;
    private static final int MAX_PAGE = 10000;

    private final UserAccountRepository accounts;
    private final DesignerReviewRepository reviews;

    public PublicDesignerReviewService(UserAccountRepository accounts, DesignerReviewRepository reviews) {
        this.accounts = accounts;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public PublicDesignerReviewsResponse list(Long designerId, int page) {
        if (page < 0 || page > MAX_PAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid review page");
        }
        // Match the public designer profile visibility rule, including inactive accounts.
        accounts.findByIdAndRoleAndActiveTrue(designerId, UserRole.INTERIOR_DESIGNER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));

        Map<Integer, Long> distribution = emptyDistribution();
        for (Object[] ratingCount : reviews.countRatingsByDesignerId(designerId)) {
            distribution.put(((Number) ratingCount[0]).intValue(), ((Number) ratingCount[1]).longValue());
        }
        long total = distribution.values().stream().mapToLong(Long::longValue).sum();
        long weighted = distribution.entrySet().stream()
                .mapToLong(item -> item.getKey() * item.getValue()).sum();
        double average = total == 0 ? 0 : Math.round((weighted * 10.0) / total) / 10.0;
        Slice<DesignerReview> slice = reviews.findByDesigner_Id(
                designerId, PageRequest.of(page, PAGE_SIZE,
                        Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new PublicDesignerReviewsResponse(total, average, distribution,
                slice.getContent().stream().map(PublicDesignerReviewResponse::from).toList(),
                page, slice.hasNext());
    }

    private static Map<Integer, Long> emptyDistribution() {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (int rating = 5; rating >= 1; rating--) counts.put(rating, 0L);
        return counts;
    }
}
