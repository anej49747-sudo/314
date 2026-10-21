package csit314.renovation.review.control;

import csit314.renovation.account.entity.UserAccount;
import csit314.renovation.account.entity.UserRole;
import csit314.renovation.account.repository.UserAccountRepository;
import csit314.renovation.review.dto.SubmitDesignerReviewRequest;
import csit314.renovation.review.dto.SubmittedDesignerReviewResponse;
import csit314.renovation.review.entity.DesignerReview;
import csit314.renovation.review.repository.DesignerReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DesignerReviewService {
    private final UserAccountRepository accounts;
    private final DesignerReviewRepository reviews;

    public DesignerReviewService(UserAccountRepository accounts, DesignerReviewRepository reviews) {
        this.accounts = accounts;
        this.reviews = reviews;
    }

    @Transactional
    public SubmittedDesignerReviewResponse submit(Long designerId, SubmitDesignerReviewRequest request,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Customer sign-in required");
        }
        UserAccount customer = accounts.findByEmailIgnoreCase(authentication.getName())
                .filter(account -> account.isActive() && account.getRole() == UserRole.CUSTOMER)
                .orElseThrow(() -> new AccessDeniedException("Active customer access required"));
        // Admin-provisioned, active designers are the public/approved designers in this app.
        UserAccount designer = accounts.findByIdAndRoleAndActiveTrue(designerId, UserRole.INTERIOR_DESIGNER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Designer not found"));
        String comment = normalizeComment(request.comment());
        DesignerReview review = reviews.saveAndFlush(
                new DesignerReview(designer, customer, request.rating(), comment));
        return SubmittedDesignerReviewResponse.from(review);
    }

    public static String normalizeComment(String comment) {
        if (comment == null) return null;
        String trimmed = comment.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
