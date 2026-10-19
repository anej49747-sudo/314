package csit314.renovation.portfolio.dto;

import csit314.renovation.portfolio.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Retained image IDs are explicit: an empty list removes all current photos. */
public record UpdateProjectRequest(
        @NotBlank(message = "Project title is required")
        @Size(max = 160, message = "Title must be 160 characters or fewer") String title,
        @NotBlank(message = "Project description is required")
        @Size(max = 3000, message = "Description must be 3000 characters or fewer") String description,
        @NotNull(message = "Choose a category")
        @Positive(message = "Choose a valid category") Long categoryId,
        @NotNull(message = "Choose a project status") ProjectStatus status,
        @NotNull(message = "Specify which existing photos to keep") List<@NotNull Long> keepImageIds) { }
