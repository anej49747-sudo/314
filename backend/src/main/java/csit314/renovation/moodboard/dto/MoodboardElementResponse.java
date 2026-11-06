package csit314.renovation.moodboard.dto;

public record MoodboardElementResponse(String kind, Long photoId, String photoUrl, boolean unavailable,
        String sourceLabel, String text, String color, Integer fontSize,
        int x, int y, int width, int height) {}
