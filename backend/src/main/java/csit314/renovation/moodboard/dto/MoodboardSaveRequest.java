package csit314.renovation.moodboard.dto;

import java.util.List;

/** Version is required on every layout edit to prevent silent overwrites. */
public record MoodboardSaveRequest(Long version, String title, String backgroundColor,
        List<MoodboardElementInput> elements) {}
