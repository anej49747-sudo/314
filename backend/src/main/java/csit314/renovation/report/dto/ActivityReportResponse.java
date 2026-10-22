package csit314.renovation.report.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ActivityReportResponse(String frequency, LocalDate startDate, LocalDate endDate,
        String timezone, long logins, long uniqueUsers, long newAccounts,
        long projectViews, long favouritesAdded, long favouritesRemoved,
        long reviewsSubmitted, boolean engagementComplete, Instant engagementTrackingStartedAt,
        String coverageNotice, List<DailyActivityResponse> days) { }
