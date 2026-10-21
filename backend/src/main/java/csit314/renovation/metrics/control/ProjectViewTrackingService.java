package csit314.renovation.metrics.control;

import csit314.renovation.metrics.repository.ProjectInterestMetricsRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/** Records one successful public-detail view per HTTP browser session and project. */
@Service
public class ProjectViewTrackingService {
    private static final String VIEW_ATTRIBUTE_PREFIX = "csit314.project-viewed.";
    private final ProjectInterestMetricsRepository metrics;

    public ProjectViewTrackingService(ProjectInterestMetricsRepository metrics) {
        this.metrics = metrics;
    }

    public void recordPublicView(Long projectId, HttpServletRequest request) {
        // An anonymous visitor receives a normal HttpOnly session cookie. The database
        // stores only aggregate counts, never the session ID or visitor IP address.
        HttpSession session = request.getSession(true);
        String marker = VIEW_ATTRIBUTE_PREFIX + projectId;
        synchronized (session) {
            if (Boolean.TRUE.equals(session.getAttribute(marker))) return;
            metrics.incrementViews(projectId);
            session.setAttribute(marker, Boolean.TRUE);
        }
    }
}
