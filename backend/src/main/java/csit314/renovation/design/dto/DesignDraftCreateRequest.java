package csit314.renovation.design.dto;

public record DesignDraftCreateRequest(String title, Integer floorWidthCm,
        Integer floorHeightCm, String template) {}
