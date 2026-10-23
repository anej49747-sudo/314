package csit314.renovation.portfolio.dto;

import csit314.renovation.portfolio.entity.InteriorDesignProject;
import csit314.renovation.portfolio.entity.ProjectStatus;

public record ManagerProjectResponse(Long id, String title, String description, Long categoryId,
        String categoryName, ProjectStatus status, Long designerId, String designerName, long imageCount) {
    public static ManagerProjectResponse from(InteriorDesignProject project, long imageCount) {
        return new ManagerProjectResponse(project.getId(), project.getTitle(), project.getDescription(),
                project.getCategoryIdOrNull(), project.getCategoryDisplayName(), project.getStatus(),
                project.getDesigner().getId(), project.getDesigner().getDisplayName(), imageCount);
    }
}
