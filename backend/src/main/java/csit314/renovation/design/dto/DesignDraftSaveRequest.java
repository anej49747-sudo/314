package csit314.renovation.design.dto;

import java.util.List;

public record DesignDraftSaveRequest(Long version, String title, Integer floorWidthCm,
        Integer floorHeightCm, List<DesignElementInput> elements) {}
