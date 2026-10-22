package csit314.renovation.dispute.boundary;

import csit314.renovation.dispute.control.DesignerDisputeService;
import csit314.renovation.dispute.dto.DesignerReportPageResponse;
import csit314.renovation.dispute.dto.DesignerReportResponse;
import csit314.renovation.dispute.dto.ResolveDesignerReportRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/designer-reports")
public class AdminDesignerReportController {
    private final DesignerDisputeService disputes;

    public AdminDesignerReportController(DesignerDisputeService disputes) {
        this.disputes = disputes;
    }

    @GetMapping
    public ResponseEntity<DesignerReportPageResponse> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(disputes.search(status, q, page, authentication));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<DesignerReportResponse> find(@PathVariable Long reportId,
            Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(disputes.find(reportId, authentication));
    }

    @PutMapping("/{reportId}/decision")
    public ResponseEntity<DesignerReportResponse> decide(@PathVariable Long reportId,
            @Valid @RequestBody ResolveDesignerReportRequest request, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(disputes.decide(reportId, request, authentication));
    }
}
