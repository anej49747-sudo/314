package csit314.renovation.review.boundary;

import csit314.renovation.review.control.DesignerReviewService;
import csit314.renovation.review.dto.SubmitDesignerReviewRequest;
import csit314.renovation.review.dto.SubmittedDesignerReviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DesignerReviewController {
    private final DesignerReviewService service;

    public DesignerReviewController(DesignerReviewService service) {
        this.service = service;
    }

    @PostMapping("/api/designers/{designerId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public SubmittedDesignerReviewResponse submit(@PathVariable Long designerId,
            @Valid @RequestBody SubmitDesignerReviewRequest request, Authentication authentication) {
        return service.submit(designerId, request, authentication);
    }
}
