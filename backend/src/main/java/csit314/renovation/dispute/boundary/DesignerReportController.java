package csit314.renovation.dispute.boundary;

import csit314.renovation.dispute.control.DesignerDisputeService;
import csit314.renovation.dispute.dto.DesignerReportResponse;
import csit314.renovation.dispute.dto.SubmitDesignerReportRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DesignerReportController {
    private final DesignerDisputeService disputes;

    public DesignerReportController(DesignerDisputeService disputes) {
        this.disputes = disputes;
    }

    @PostMapping("/api/designers/{designerId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public DesignerReportResponse submit(@PathVariable Long designerId,
            @Valid @RequestBody SubmitDesignerReportRequest request, Authentication authentication) {
        return disputes.submit(designerId, request, authentication);
    }
}
