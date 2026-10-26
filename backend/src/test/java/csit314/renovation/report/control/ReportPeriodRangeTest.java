package csit314.renovation.report.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ReportPeriodRangeTest {
    @Test
    void supportsSingleDayAndCrossMonthPeriods() {
        var single = ReportPeriod.range("2026-09-01", "2026-09-01");
        assertEquals("2026-09-01", single.startDate().toString());
        assertEquals("2026-09-01", single.endDate().toString());
        assertEquals(86400, single.toExclusive().getEpochSecond() - single.fromInclusive().getEpochSecond());
        var longRange = ReportPeriod.range("2026-08-29", "2026-10-02");
        assertEquals("2026-10-02", longRange.endDate().toString());
    }


    @Test
    void inclusiveRangeUsesSingaporeCalendarBoundariesAndRejectsInvalidDates() {
        var period = ReportPeriod.range("2026-08-31", "2026-09-01");
        assertEquals("2026-08-30T16:00:00Z", period.fromInclusive().toString());
        assertEquals("2026-09-01T16:00:00Z", period.toExclusive().toString());
        assertThrows(IllegalArgumentException.class,
                () -> ReportPeriod.range("2026-02-30", "2026-03-01"));
        assertThrows(IllegalArgumentException.class,
                () -> ReportPeriod.range("bad-date", "2026-03-01"));
    }

    @Test
    void rejectsReverseDates() {
        assertThrows(IllegalArgumentException.class,
                () -> ReportPeriod.range("2026-09-03", "2026-09-02"));
    }
}
