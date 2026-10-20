package csit314.renovation.designer.dto;

import java.util.List;

/** Public profile fields only. Never serialize UserAccount or a JPA entity. */
public record DesignerProfileResponse(Long id, String name, String address, String description,
        List<PublicPortfolioProjectResponse> projects) {}
