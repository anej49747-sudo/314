package csit314.renovation.dispute.dto;

import csit314.renovation.dispute.entity.DesignerReport;
import java.time.Instant;

/** Private response only; never expose a customer's report on a public API. */
public record DesignerReportResponse(Long id, Long designerId, String designerName,
        Long customerId, String customerName, String reason, String description,
        String status, Instant submittedAt, String decisionNote,
        Long decidedById, Instant decidedAt) {
    public static DesignerReportResponse from(DesignerReport report, String designerName) {
        return new DesignerReportResponse(report.getId(), report.getDesigner().getId(),
                designerName, report.getCustomer().getId(),
                report.getCustomer().getDisplayName(), report.getReason().name(),
                report.getDescription(), report.getStatus().name(), report.getSubmittedAt(),
                report.getDecisionNote(), report.getDecidedBy() == null ? null : report.getDecidedBy().getId(),
                report.getDecidedAt());
    }
}
