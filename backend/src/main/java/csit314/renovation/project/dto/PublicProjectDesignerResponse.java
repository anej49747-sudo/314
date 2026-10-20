package csit314.renovation.project.dto;

/** Do not expose contact details or serialize UserAccount/DesignerCompany entities. */
public record PublicProjectDesignerResponse(Long id, String name, String address) {}
