package csit314.renovation.report.control;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/** Inclusive Singapore dates, represented as half-open UTC instants for SQL. */
public record ReportPeriod(String frequency, LocalDate startDate, LocalDate endDate,
        Instant fromInclusive, Instant toExclusive) {
    public static final ZoneId SINGAPORE = ZoneId.of("Asia/Singapore");

    public static ReportPeriod of(String frequencyValue, String selectedDate) {
        String frequency = frequencyValue == null ? "" : frequencyValue.strip().toLowerCase(Locale.ROOT);
        if (!frequency.equals("daily") && !frequency.equals("weekly") && !frequency.equals("monthly")) {
            throw new IllegalArgumentException("Report frequency must be daily, weekly or monthly");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(selectedDate);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new IllegalArgumentException("Select a valid ISO date (yyyy-MM-dd)");
        }
        LocalDate first = switch (frequency) {
            case "weekly" -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "monthly" -> date.withDayOfMonth(1);
            default -> date;
        };
        LocalDate next = switch (frequency) {
            case "weekly" -> first.plusWeeks(1);
            case "monthly" -> first.plusMonths(1);
            default -> first.plusDays(1);
        };
        return new ReportPeriod(frequency, first, next.minusDays(1),
                first.atStartOfDay(SINGAPORE).toInstant(),
                next.atStartOfDay(SINGAPORE).toInstant());
    }
}
