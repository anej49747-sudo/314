package csit314.renovation.design.dto;

import java.time.Instant;

public record DesignDraftSummaryResponse(Long id, String title, int floorWidthCm,
        int floorHeightCm, int roomCount, Instant updatedAt) {}
