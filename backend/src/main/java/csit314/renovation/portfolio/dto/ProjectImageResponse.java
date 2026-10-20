package csit314.renovation.portfolio.dto;

import csit314.renovation.portfolio.entity.ProjectImage;

public record ProjectImageResponse(Long id, String url) {
    public static ProjectImageResponse from(ProjectImage image) {
        return new ProjectImageResponse(image.getId(),
                "/api/designer/projects/" + image.getProjectId() + "/images/" + image.getId());
    }
}
