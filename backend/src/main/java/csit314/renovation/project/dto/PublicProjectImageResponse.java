package csit314.renovation.project.dto;

import csit314.renovation.portfolio.entity.ProjectImage;

/** Public image URL only: never disclose the storage key or private designer endpoint. */
public record PublicProjectImageResponse(Long id, String url) {
    public static PublicProjectImageResponse from(ProjectImage image) {
        return new PublicProjectImageResponse(image.getId(),
                "/api/projects/" + image.getProjectId() + "/images/" + image.getId());
    }
}
