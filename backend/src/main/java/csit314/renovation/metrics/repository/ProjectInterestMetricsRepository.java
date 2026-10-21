package csit314.renovation.metrics.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Stores only aggregate views; favourite totals are calculated from current saves. */
@Repository
public class ProjectInterestMetricsRepository {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate namedJdbc;

    public ProjectInterestMetricsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.namedJdbc = new NamedParameterJdbcTemplate(jdbc);
    }

    public void incrementViews(Long projectId) {
        jdbc.update("INSERT INTO project_view_counts (project_id, view_count) VALUES (?, 1) "
                + "ON CONFLICT (project_id) DO UPDATE "
                + "SET view_count = project_view_counts.view_count + 1", projectId);
    }

    public Map<Long, ProjectCounts> countsForProjects(List<Long> projectIds) {
        if (projectIds.isEmpty()) return Map.of();

        Map<Long, Long> views = new HashMap<>();
        namedJdbc.query("SELECT project_id, view_count FROM project_view_counts "
                        + "WHERE project_id IN (:ids)", Map.of("ids", projectIds),
                result -> {
                    views.put(result.getLong("project_id"), result.getLong("view_count"));
                });

        Map<Long, Long> favourites = new HashMap<>();
        namedJdbc.query("SELECT project_id, COUNT(*) AS favourite_count "
                        + "FROM customer_project_favourites WHERE project_id IN (:ids) "
                        + "GROUP BY project_id", Map.of("ids", projectIds),
                result -> {
                    favourites.put(result.getLong("project_id"),
                            result.getLong("favourite_count"));
                });

        Map<Long, ProjectCounts> counts = new HashMap<>();
        for (Long id : projectIds) {
            counts.put(id, new ProjectCounts(views.getOrDefault(id, 0L),
                    favourites.getOrDefault(id, 0L)));
        }
        return counts;
    }

    public record ProjectCounts(long viewCount, long favouriteCount) { }
}
