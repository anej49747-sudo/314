package csit314.renovation.report.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import csit314.renovation.authentication.control.AuthenticationService;
import csit314.renovation.report.dto.ActivityReportResponse;
import csit314.renovation.report.repository.ActivityReportRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class ActivityReportServiceTest {
    @Mock AuthenticationService authenticationService;
    @Mock ActivityReportRepository reports;
    @Mock Authentication authentication;
    @InjectMocks ActivityReportService service;

    @Test
    void distinctPeriodUsersAreNotSumOfDailyUniques() {
        LocalDate first = LocalDate.of(2026, 10, 12);
        when(reports.engagementStartedAt()).thenReturn(Instant.parse("2026-10-01T00:00:00Z"));
        when(reports.dailyUniqueUsers(any())).thenReturn(Map.of(first, 2L, first.plusDays(1), 2L));
        when(reports.periodUniqueUsers(any())).thenReturn(3L);
        when(reports.successfulLogins(any())).thenReturn(Map.of(first, 3L, first.plusDays(1), 2L));
        ActivityReportResponse result = service.generate("weekly", "2026-10-13", authentication);
        assertEquals(5, result.logins());
        assertEquals(3, result.uniqueUsers());
        assertEquals(4, result.days().stream().mapToLong(day -> day.uniqueUsers()).sum());
        assertTrue(result.engagementComplete());
        assertEquals(7, result.days().size());
    }

    @Test
    void periodOverlappingTrackingStartIsExplicitlyMarkedIncomplete() {
        when(reports.engagementStartedAt()).thenReturn(Instant.parse("2026-10-22T11:00:00Z"));
        when(reports.engagement(any(), eq("PROJECT_VIEW"))).thenReturn(
                Map.of(LocalDate.of(2026, 10, 22), 3L));
        ActivityReportResponse result = service.generate("daily", "2026-10-22", authentication);
        assertFalse(result.engagementComplete());
        assertFalse(result.days().getFirst().engagementComplete());
        assertEquals(3, result.projectViews());
    }
}
