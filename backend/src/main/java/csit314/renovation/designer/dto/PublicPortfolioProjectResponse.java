package csit314.renovation.designer.dto;

import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;

/** Safe portfolio card fields, including a public photo URL (never a storage key). */
public record PublicPortfolioProjectResponse(Long id, String title, String categoryName,
        ProjectStatus status, String coverImageUrl) {
    public static PublicPortfolioProjectResponse from(InteriorDesignProject project, String coverUrl) {
        return new PublicPortfolioProjectResponse(project.getId(), project.getTitle(),
                project.getCategoryDisplayName(), project.getStatus(), coverUrl);
    }
}
