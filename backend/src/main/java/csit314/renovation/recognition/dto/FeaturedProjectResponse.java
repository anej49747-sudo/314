package csit314.renovation.recognition.dto;

import csit314.renovation.recognition.entity.ProjectOfMonth;
import java.time.Instant;

public record FeaturedProjectResponse(String month, Long projectId, String projectTitle,
        Long designerId, Instant selectedAt, String coverUrl) {
    public static FeaturedProjectResponse from(ProjectOfMonth featured) {
        var project = featured.getProject();
        return new FeaturedProjectResponse(featured.getMonth(), project.getId(), project.getTitle(),
                project.getDesigner().getId(), featured.getSelectedAt(), null);
    }
    public FeaturedProjectResponse withCover(String url) {
        return new FeaturedProjectResponse(month, projectId, projectTitle, designerId, selectedAt, url);
    }
}
