package csit314.renovation.dispute.dto;

import csit314.renovation.dispute.entity.DesignerReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitDesignerReportRequest(
        @NotNull DesignerReportReason reason,
        @NotBlank @Size(min = 20, max = 2000) String description) {}
