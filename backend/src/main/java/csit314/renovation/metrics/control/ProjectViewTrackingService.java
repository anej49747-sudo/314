package csit314.renovation.metrics.control;

import csit314.renovation.metrics.repository.ProjectInterestMetricsRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import csit314.renovation.report.repository.PlatformEngagementRecorder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

/** Records one successful public-detail view per HTTP browser session and project. */
@Service
public class ProjectViewTrackingService {
    private static final String VIEW_ATTRIBUTE_PREFIX = "csit314.project-viewed.";
    private final ProjectInterestMetricsRepository metrics;
    private final PlatformEngagementRecorder engagement;

    public ProjectViewTrackingService(ProjectInterestMetricsRepository metrics,
            PlatformEngagementRecorder engagement) {
        this.metrics = metrics;
        this.engagement = engagement;
    }

    @Transactional
    public void recordPublicView(Long projectId, HttpServletRequest request) {
        // An anonymous visitor receives a normal HttpOnly session cookie. The database
        // stores only aggregate counts, never the session ID or visitor IP address.
        HttpSession session = request.getSession(true);
        String marker = VIEW_ATTRIBUTE_PREFIX + projectId;
        synchronized (session) {
            if (Boolean.TRUE.equals(session.getAttribute(marker))) return;
            metrics.incrementViews(projectId);
            engagement.recordProjectView();
            session.setAttribute(marker, Boolean.TRUE);
        }
    }
}
