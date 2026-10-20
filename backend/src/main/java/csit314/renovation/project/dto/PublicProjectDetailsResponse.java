package csit314.renovation.project.dto;

import csit314.renovation.portfolio.entity.ProjectStatus;

/** Deliberately limited to public-facing project and designer fields. */
public record PublicProjectDetailsResponse(Long id, String title, String description,
        String categoryName, ProjectStatus status, PublicProjectDesignerResponse designer) {}
