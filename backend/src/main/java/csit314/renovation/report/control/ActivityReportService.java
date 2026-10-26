package csit314.renovation.report.control;

import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.report.dto.ActivityReportResponse;
import csit314.renovation.report.dto.DailyActivityResponse;
import csit314.renovation.report.dto.RangeActivityDay;
import csit314.renovation.report.dto.RangeActivitySummary;
import csit314.renovation.report.dto.RangeActivityResponse;
import csit314.renovation.report.repository.ActivityReportRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityReportService {
    private final AuthenticationService authenticationService;
    private final ActivityReportRepository reports;

    public ActivityReportService(AuthenticationService authenticationService,
            ActivityReportRepository reports) {
        this.authenticationService = authenticationService;
        this.reports = reports;
    }

    @Transactional(readOnly = true)
    public ActivityReportResponse generate(String frequency, String date, Authentication authentication) {
        authenticationService.currentManager(authentication);
        ReportPeriod period = ReportPeriod.of(frequency, date);
        Instant start = reports.engagementStartedAt();
        Map<LocalDate, Long> logins = reports.successfulLogins(period);
        Map<LocalDate, Long> unique = reports.dailyUniqueUsers(period);
        Map<LocalDate, Long> newAccounts = reports.newAccounts(period);
        Map<LocalDate, Long> views = reports.engagement(period, "PROJECT_VIEW");
        Map<LocalDate, Long> added = reports.engagement(period, "FAVOURITE_ADDED");
        Map<LocalDate, Long> removed = reports.engagement(period, "FAVOURITE_REMOVED");
        Map<LocalDate, Long> reviews = reports.reviews(period);

        List<DailyActivityResponse> days = new ArrayList<>();
        for (LocalDate d = period.startDate(); !d.isAfter(period.endDate()); d = d.plusDays(1)) {
            boolean complete = !d.atStartOfDay(ReportPeriod.SINGAPORE).toInstant().isBefore(start);
            days.add(new DailyActivityResponse(d, at(logins, d), at(unique, d),
                    at(newAccounts, d), at(views, d), at(added, d), at(removed, d),
                    at(reviews, d), complete));
        }
        boolean complete = !period.fromInclusive().isBefore(start);
        return new ActivityReportResponse(period.frequency(), period.startDate(), period.endDate(),
                ReportPeriod.SINGAPORE.getId(), total(days, DailyActivityResponse::logins),
                reports.periodUniqueUsers(period), total(days, DailyActivityResponse::newAccounts),
                total(days, DailyActivityResponse::projectViews),
                total(days, DailyActivityResponse::favouritesAdded),
                total(days, DailyActivityResponse::favouritesRemoved),
                total(days, DailyActivityResponse::reviewsSubmitted), complete, start,
                "Engagement events are recorded only from Story #32 tracking; periods before tracking "
                + "started are incomplete. Login history began with Story #41; older login attempts "
                + "cannot be reconstructed. Anonymous totals are retained when accounts are deleted.",
                days);
    }

    @Transactional(readOnly = true)
    public RangeActivityResponse generateRange(String startDate, String endDate,
            Authentication authentication) {
        authenticationService.currentManager(authentication);
        ReportPeriod period = ReportPeriod.range(startDate, endDate);
        Map<LocalDate, Long> customerLogins = reports.roleLogins(period, "CUSTOMER", false);
        Map<LocalDate, Long> customerUnique = reports.roleLogins(period, "CUSTOMER", true);
        Map<LocalDate, Long> customerNew = reports.roleRegistrations(period, "CUSTOMER");
        Map<LocalDate, Long> designerLogins = reports.roleLogins(period, "INTERIOR_DESIGNER", false);
        Map<LocalDate, Long> designerUnique = reports.roleLogins(period, "INTERIOR_DESIGNER", true);
        Map<LocalDate, Long> designerNew = reports.roleRegistrations(period, "INTERIOR_DESIGNER");
        Map<LocalDate, Long> designerProjects = reports.designerProjectsCreated(period);
        Map<LocalDate, Long> projectViews = reports.engagement(period, "PROJECT_VIEW");
        Map<LocalDate, Long> favouritesAdded = reports.engagement(period, "FAVOURITE_ADDED");
        Map<LocalDate, Long> reviews = reports.reviews(period);
        Instant trackingStart = reports.engagementStartedAt();
        List<RangeActivityDay> days = new ArrayList<>();
        for (LocalDate d = period.startDate(); !d.isAfter(period.endDate()); d = d.plusDays(1)) {
            boolean complete = !d.atStartOfDay(ReportPeriod.SINGAPORE).toInstant().isBefore(trackingStart);
            days.add(new RangeActivityDay(d, at(customerLogins, d), at(customerUnique, d),
                    at(customerNew, d), at(designerLogins, d), at(designerUnique, d),
                    at(designerNew, d), at(designerProjects, d), at(projectViews, d),
                    at(favouritesAdded, d), at(reviews, d), complete));
        }
        RangeActivitySummary customer = new RangeActivitySummary(total(days, RangeActivityDay::customerLogins),
                reports.periodRoleUniqueUsers(period, "CUSTOMER"),
                total(days, RangeActivityDay::customerNewAccounts), 0L);
        RangeActivitySummary designer = new RangeActivitySummary(total(days, RangeActivityDay::designerLogins),
                reports.periodRoleUniqueUsers(period, "INTERIOR_DESIGNER"),
                total(days, RangeActivityDay::designerNewAccounts),
                total(days, RangeActivityDay::designerProjectsCreated));
        boolean complete = !period.fromInclusive().isBefore(trackingStart);
        return new RangeActivityResponse(period.startDate(), period.endDate(),
                ReportPeriod.SINGAPORE.getId(), customer, designer,
                total(days, RangeActivityDay::projectViews),
                total(days, RangeActivityDay::favouritesAdded),
                total(days, RangeActivityDay::reviewsSubmitted), complete,
                "Project views and favourite events are anonymous site-wide events, not assigned to a role. "
                + "Role-specific login and account totals exclude records for deleted accounts whose roles are no longer available. "
                + "Events before tracking began may be incomplete.", days);
    }

    private static long at(Map<LocalDate, Long> entries, LocalDate date) {
        return entries.getOrDefault(date, 0L);
    }

    private static <T> long total(List<T> days,
            java.util.function.ToLongFunction<? super T> counter) {
        return days.stream().mapToLong(counter).sum();
    }
}
