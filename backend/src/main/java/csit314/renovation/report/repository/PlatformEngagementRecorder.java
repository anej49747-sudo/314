package csit314.renovation.report.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Records only real changes. Anonymous views do not identify visitors. */
@Repository
public class PlatformEngagementRecorder {
    private final JdbcTemplate jdbc;

    public PlatformEngagementRecorder(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void recordProjectView() { record("PROJECT_VIEW"); }
    public void recordFavouriteAdded() { record("FAVOURITE_ADDED"); }
    public void recordFavouriteRemoved() { record("FAVOURITE_REMOVED"); }

    private void record(String type) {
        jdbc.update("INSERT INTO platform_engagement_events (event_type) VALUES (?)", type);
    }
}
