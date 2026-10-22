package csit314.renovation.report.repository;

import csit314.renovation.report.control.ReportPeriod;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read-only period counts; PostgreSQL calculates day boundaries in Singapore time. */
@Repository
public class ActivityReportRepository {
    private static final String CUSTOMER_DESIGNER = "('CUSTOMER', 'INTERIOR_DESIGNER')";
    private final JdbcTemplate jdbc;

    public ActivityReportRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Instant engagementStartedAt() {
        return jdbc.queryForObject("SELECT started_at FROM platform_activity_tracking "
                        + "WHERE metric = 'ENGAGEMENT'", (rs, row) ->
                        rs.getTimestamp(1).toInstant());
    }

    public Map<LocalDate, Long> successfulLogins(ReportPeriod period) {
        return daily("SELECT (e.occurred_at AT TIME ZONE 'Asia/Singapore')::date AS report_day, "
                + "COUNT(*) AS total FROM user_login_events e "
                + "JOIN user_accounts a ON a.id = e.account_id "
                + "WHERE e.successful = true AND a.role IN " + CUSTOMER_DESIGNER
                + " AND e.occurred_at >= ? AND e.occurred_at < ? GROUP BY report_day", period);
    }

    public Map<LocalDate, Long> dailyUniqueUsers(ReportPeriod period) {
        return daily("SELECT (e.occurred_at AT TIME ZONE 'Asia/Singapore')::date AS report_day, "
                + "COUNT(DISTINCT e.account_id) AS total FROM user_login_events e "
                + "JOIN user_accounts a ON a.id = e.account_id "
                + "WHERE e.successful = true AND a.role IN " + CUSTOMER_DESIGNER
                + " AND e.occurred_at >= ? AND e.occurred_at < ? GROUP BY report_day", period);
    }

    public long periodUniqueUsers(ReportPeriod period) {
        Long count = jdbc.queryForObject("SELECT COUNT(DISTINCT e.account_id) "
                + "FROM user_login_events e JOIN user_accounts a ON a.id = e.account_id "
                + "WHERE e.successful = true AND a.role IN " + CUSTOMER_DESIGNER
                + " AND e.occurred_at >= ? AND e.occurred_at < ?", Long.class,
                Timestamp.from(period.fromInclusive()), Timestamp.from(period.toExclusive()));
        return count == null ? 0L : count;
    }

    public Map<LocalDate, Long> newAccounts(ReportPeriod period) {
        return daily("SELECT (a.created_at AT TIME ZONE 'Asia/Singapore')::date AS report_day, "
                + "COUNT(*) AS total FROM user_accounts a WHERE a.role IN " + CUSTOMER_DESIGNER
                + " AND a.created_at >= ? AND a.created_at < ? GROUP BY report_day", period);
    }

    public Map<LocalDate, Long> reviews(ReportPeriod period) {
        return daily("SELECT (r.created_at AT TIME ZONE 'Asia/Singapore')::date AS report_day, "
                + "COUNT(*) AS total FROM designer_reviews r "
                + "WHERE r.created_at >= ? AND r.created_at < ? GROUP BY report_day", period);
    }

    public Map<LocalDate, Long> engagement(ReportPeriod period, String eventType) {
        String sql = "SELECT (e.occurred_at AT TIME ZONE 'Asia/Singapore')::date AS report_day, "
                + "COUNT(*) AS total FROM platform_engagement_events e "
                + "WHERE e.event_type = ? AND e.occurred_at >= ? AND e.occurred_at < ? "
                + "GROUP BY report_day";
        Map<LocalDate, Long> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getDate("report_day").toLocalDate(), rs.getLong("total"));
        }, eventType,
                Timestamp.from(period.fromInclusive()), Timestamp.from(period.toExclusive()));
        return result;
    }

    private Map<LocalDate, Long> daily(String sql, ReportPeriod period) {
        Map<LocalDate, Long> result = new HashMap<>();
        jdbc.query(sql, rs -> {
            result.put(rs.getDate("report_day").toLocalDate(), rs.getLong("total"));
        }, Timestamp.from(period.fromInclusive()),
                Timestamp.from(period.toExclusive()));
        return result;
    }
}
