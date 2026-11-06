package csit314.renovation.design.dto;

import java.time.Instant;
import java.util.List;

public record DesignDraftResponse(Long id, String title, int floorWidthCm, int floorHeightCm,
        Long version, Instant updatedAt, int roomOverlapCount, List<DesignElementResponse> elements) {}
