package csit314.renovation.report.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReportPeriodTest {
    @Test
    void dayBoundariesUseSingaporeInsteadOfUtcMidnight() {
        ReportPeriod period = ReportPeriod.of("daily", "2026-10-22");
        assertEquals(Instant.parse("2026-10-21T16:00:00Z"), period.fromInclusive());
        assertEquals(Instant.parse("2026-10-22T16:00:00Z"), period.toExclusive());
    }

    @Test
    void weekCrossesYearFromMondayToSunday() {
        ReportPeriod period = ReportPeriod.of("weekly", "2027-01-01");
        assertEquals(LocalDate.of(2026, 12, 28), period.startDate());
        assertEquals(LocalDate.of(2027, 1, 3), period.endDate());
    }

    @Test
    void monthlyHandlesLeapYear() {
        ReportPeriod period = ReportPeriod.of("monthly", "2028-02-29");
        assertEquals(LocalDate.of(2028, 2, 1), period.startDate());
        assertEquals(LocalDate.of(2028, 2, 29), period.endDate());
    }

    @Test
    void invalidFrequencyOrDateIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ReportPeriod.of("annual", "2026-10-22"));
        assertThrows(IllegalArgumentException.class, () -> ReportPeriod.of("daily", "2026-02-30"));
    }
}
