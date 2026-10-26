package csit314.renovation.report.dto;

import java.time.LocalDate;
import java.util.List;

public record RangeActivityResponse(LocalDate startDate, LocalDate endDate, String timezone,
        RangeActivitySummary customer, RangeActivitySummary designer,
        long projectViews, long favouritesAdded, long reviewsSubmitted,
        boolean engagementComplete, String coverageNotice, List<RangeActivityDay> days) { }
