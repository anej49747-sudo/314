package csit314.renovation.portfolio.dto;

import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;
import java.time.Instant;

public record PortfolioProjectResponse(Long id, String title, String description,
        Long categoryId, String categoryName, ProjectStatus status, Instant createdAt) {

    public static PortfolioProjectResponse from(InteriorDesignProject project) {
        return new PortfolioProjectResponse(project.getId(), project.getTitle(),
                project.getDescription(), project.getCategory().getId(),
                project.getCategory().getName(), project.getStatus(), project.getCreatedAt());
    }
}
