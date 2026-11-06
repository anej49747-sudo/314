package csit314.renovation.design.dto;

public record DesignElementInput(String kind, String name, Integer xCm, Integer yCm,
        Integer widthCm, Integer heightCm, Integer rotation) {}
