package csit314.renovation.review.boundary;

import csit314.renovation.review.control.PublicDesignerReviewService;
import csit314.renovation.review.dto.PublicDesignerReviewsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicDesignerReviewController {
    private final PublicDesignerReviewService service;

    public PublicDesignerReviewController(PublicDesignerReviewService service) {
        this.service = service;
    }

    @GetMapping("/api/designers/{designerId}/reviews")
    public PublicDesignerReviewsResponse list(@PathVariable Long designerId,
            @RequestParam(defaultValue = "0") int page) {
        return service.list(designerId, page);
    }
}
