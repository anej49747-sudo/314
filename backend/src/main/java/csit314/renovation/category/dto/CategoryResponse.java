package csit314.renovation.category.dto;

import csit314.renovation.category.entity.ProjectCategory;

public record CategoryResponse(Long id, String name) {
    public static CategoryResponse from(ProjectCategory category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
