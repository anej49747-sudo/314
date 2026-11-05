package csit314.renovation.portfolio.dto;

import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PortfolioProjectResponse(Long id, String title, String description,
        Long categoryId, String categoryName, ProjectStatus status, Instant createdAt, List<ProjectImageResponse> images,
        long viewCount, long favouriteCount, Map<String,Object> metadata) {

    public static PortfolioProjectResponse from(InteriorDesignProject project, List<ProjectImageResponse> images) {
        return from(project, images, 0L, 0L);
    }

    public static PortfolioProjectResponse from(InteriorDesignProject project, List<ProjectImageResponse> images,
            long viewCount, long favouriteCount) {
        return from(project,images,viewCount,favouriteCount,Map.of());
    }

    public static PortfolioProjectResponse from(InteriorDesignProject project, List<ProjectImageResponse> images,
            long viewCount, long favouriteCount, Map<String,Object> metadata) {
        return new PortfolioProjectResponse(project.getId(), project.getTitle(),
                project.getDescription(), project.getCategoryIdOrNull(),
                project.getCategoryDisplayName(), project.getStatus(), project.getCreatedAt(), images,
                viewCount, favouriteCount, metadata);
    }
}
