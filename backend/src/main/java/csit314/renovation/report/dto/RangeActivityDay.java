package csit314.renovation.report.dto;

import java.time.LocalDate;

/** Activity recorded on one Singapore calendar date. */
public record RangeActivityDay(LocalDate date, long customerLogins, long customerUniqueUsers,
        long customerNewAccounts, long designerLogins, long designerUniqueUsers,
        long designerNewAccounts, long designerProjectsCreated, long projectViews,
        long favouritesAdded, long reviewsSubmitted, boolean engagementComplete) { }
