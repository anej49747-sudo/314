package csit314.renovation.designer.dto;

/** Only fields approved for the public designer directory; never serialize UserAccount. */
public record DesignerDirectoryResponse(Long id, String name, String address, String description) {}
