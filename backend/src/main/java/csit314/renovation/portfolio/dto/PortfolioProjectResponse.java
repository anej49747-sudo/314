package csit314.renovation.portfolio.dto;

import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;
import java.time.Instant;
import java.util.List;

public record PortfolioProjectResponse(Long id, String title, String description,
        Long categoryId, String categoryName, ProjectStatus status, Instant createdAt, List<ProjectImageResponse> images,
        long viewCount, long favouriteCount) {

    public static PortfolioProjectResponse from(InteriorDesignProject project, List<ProjectImageResponse> images) {
        return from(project, images, 0L, 0L);
    }

    public static PortfolioProjectResponse from(InteriorDesignProject project, List<ProjectImageResponse> images,
            long viewCount, long favouriteCount) {
        return new PortfolioProjectResponse(project.getId(), project.getTitle(),
                project.getDescription(), project.getCategory().getId(),
                project.getCategory().getName(), project.getStatus(), project.getCreatedAt(), images,
                viewCount, favouriteCount);
    }
}
