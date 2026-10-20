package csit314.renovation.project.dto;

import csit314.renovation.portfolio.entity.ProjectStatus;

/** A limited public listing DTO; never serialize account or project JPA entities. */
public record PublicProjectSearchResponse(Long id, String title, String description,
        String categoryName, ProjectStatus status, PublicProjectDesignerResponse designer) {}
