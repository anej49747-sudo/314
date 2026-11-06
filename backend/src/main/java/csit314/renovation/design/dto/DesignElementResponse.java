package csit314.renovation.design.dto;

public record DesignElementResponse(String kind, String name, int xCm, int yCm,
        int widthCm, int heightCm, int rotation) {}
