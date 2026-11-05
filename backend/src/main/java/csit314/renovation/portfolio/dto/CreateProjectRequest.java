package csit314.renovation.portfolio.dto;

import csit314.renovation.portfolio.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(message = "Project title is required")
        @Size(max = 160, message = "Title must be 160 characters or fewer")
        String title,
        @NotBlank(message = "Project description is required")
        @Size(max = 3000, message = "Description must be 3000 characters or fewer")
        String description,
        @Positive(message = "Choose a valid category")
        Long categoryId,
        @NotNull(message = "Choose a project status")
        ProjectStatus status,
        csit314.renovation.discovery.control.DiscoveryService.MetadataInput metadata) {
    public CreateProjectRequest(String title,String description,Long categoryId,ProjectStatus status) {
        this(title,description,categoryId,status,null);
    }
}
