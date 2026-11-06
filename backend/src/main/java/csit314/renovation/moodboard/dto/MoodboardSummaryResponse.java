package csit314.renovation.moodboard.dto;

import java.time.Instant;

public record MoodboardSummaryResponse(Long id, String title, Instant updatedAt,
        int elementCount, String coverUrl) {}
