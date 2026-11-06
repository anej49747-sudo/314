package csit314.renovation.moodboard.dto;

public record MoodboardElementInput(String kind, Long photoId, String sourceLabel,
        String text, String color, Integer fontSize, Integer x, Integer y, Integer width, Integer height) {}
