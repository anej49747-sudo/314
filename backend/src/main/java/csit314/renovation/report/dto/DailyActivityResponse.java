package csit314.renovation.report.dto;

import java.time.LocalDate;

/** A count is an observed event count; engagementComplete indicates coverage. */
public record DailyActivityResponse(LocalDate date, long logins, long uniqueUsers,
        long newAccounts, long projectViews, long favouritesAdded,
        long favouritesRemoved, long reviewsSubmitted, boolean engagementComplete) { }
