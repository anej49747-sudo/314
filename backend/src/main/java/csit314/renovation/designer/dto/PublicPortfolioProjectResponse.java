package csit314.renovation.designer.dto;

import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;

/** Only fields needed for a portfolio preview; project detail belongs to Story #6. */
public record PublicPortfolioProjectResponse(Long id, String title, String categoryName,
        ProjectStatus status) {
    public static PublicPortfolioProjectResponse from(InteriorDesignProject project) {
        return new PublicPortfolioProjectResponse(project.getId(), project.getTitle(),
                project.getCategory().getName(), project.getStatus());
    }
}
