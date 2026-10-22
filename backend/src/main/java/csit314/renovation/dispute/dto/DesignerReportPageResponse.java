package csit314.renovation.dispute.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record DesignerReportPageResponse(List<DesignerReportResponse> reports, int page,
        int size, long totalElements, int totalPages, long openCount,
        long resolvedCount, long dismissedCount) {
    public static DesignerReportPageResponse from(Page<DesignerReportResponse> page,
            long openCount, long resolvedCount, long dismissedCount) {
        return new DesignerReportPageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), openCount, resolvedCount, dismissedCount);
    }
}
