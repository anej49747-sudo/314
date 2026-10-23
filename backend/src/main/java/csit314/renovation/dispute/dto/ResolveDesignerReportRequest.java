package csit314.renovation.dispute.dto;

import csit314.renovation.dispute.entity.DesignerReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveDesignerReportRequest(
        @NotNull DesignerReportStatus outcome,
        @NotBlank @Size(min = 10, max = 1000) String note) {}
