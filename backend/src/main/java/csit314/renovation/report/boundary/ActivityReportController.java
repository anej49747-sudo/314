package csit314.renovation.report.boundary;

import csit314.renovation.report.control.ActivityReportService;
import csit314.renovation.report.dto.ActivityReportResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager")
public class ActivityReportController {
    private final ActivityReportService reports;

    public ActivityReportController(ActivityReportService reports) { this.reports = reports; }

    @GetMapping("/reports/activity")
    public ResponseEntity<ActivityReportResponse> report(
            @RequestParam String frequency, @RequestParam String date,
            Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(reports.generate(frequency, date, authentication));
    }
}
