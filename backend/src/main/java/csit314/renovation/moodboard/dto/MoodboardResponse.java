package csit314.renovation.moodboard.dto;

import java.time.Instant;
import java.util.List;

public record MoodboardResponse(Long id, String title, String backgroundColor, Long version,
        Instant updatedAt, List<MoodboardElementResponse> elements) {}
