package csit314.renovation.report.control;

import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.report.dto.ActivityReportResponse;
import csit314.renovation.report.dto.DailyActivityResponse;
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
                + "cannot be reconstructed. Review and account creation timestamps may predate both.",
                days);
    }

    private static long at(Map<LocalDate, Long> entries, LocalDate date) {
        return entries.getOrDefault(date, 0L);
    }

    private static long total(List<DailyActivityResponse> days,
            java.util.function.ToLongFunction<DailyActivityResponse> counter) {
        return days.stream().mapToLong(counter).sum();
    }
}
