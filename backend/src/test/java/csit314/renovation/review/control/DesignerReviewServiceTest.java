package csit314.renovation.review.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DesignerReviewServiceTest {
    @Test
    void optionalCommentsAreTrimmedOrStoredAsNull() {
        assertNull(DesignerReviewService.normalizeComment(null));
        assertNull(DesignerReviewService.normalizeComment(" \n\t "));
        assertEquals("Great work", DesignerReviewService.normalizeComment("  Great work  "));
    }
}
