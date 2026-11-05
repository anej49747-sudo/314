package csit314.renovation.project.dto;

import csit314.renovation.portfolio.entity.ProjectStatus;

/** A limited public listing DTO; never serialize account or project JPA entities. */
public record PublicProjectSearchResponse(Long id, String title, String description,
        String categoryName, ProjectStatus status, PublicProjectDesignerResponse designer, String coverImageUrl,
        java.util.Map<String,Object> metadata) {
    public PublicProjectSearchResponse(Long id,String title,String description,String categoryName,ProjectStatus status,
            PublicProjectDesignerResponse designer,String coverImageUrl){
        this(id,title,description,categoryName,status,designer,coverImageUrl,java.util.Map.of());
    }
}
